#!/usr/bin/env python3
"""Bundle the approved howling portraits as static Android launcher icons.

The source sheets are retained unchanged in tools/dog_howl_source. This code
selects each approved portrait when generating the app's icon resources.
"""
from __future__ import annotations

import base64
import io
import json
from pathlib import Path
import xml.etree.ElementTree as ET

import cairo
import gi
from PIL import Image, ImageChops, ImageDraw, ImageOps

gi.require_version('Rsvg', '2.0')
from gi.repository import Rsvg

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'tools/dog_howl_source'
NEW_SOURCE = ROOT / 'tools/dog_catalog_80_source'
PLAN = ROOT / 'tools/dog_catalog_80_plan.json'
RES = ROOT / 'app/src/main/res'
ASSETS = ROOT / 'app/src/main/assets/dog-icons'
MANIFEST = ROOT / 'app/src/main/AndroidManifest.xml'
APPEARANCE = ROOT / 'app/src/main/java/app/daybreak/Appearance.java'
DENSITIES = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
BEGIN = '<!-- BEGIN GENERATED DOG HOWL ICON ALIASES -->'
END = '<!-- END GENERATED DOG HOWL ICON ALIASES -->'
# Row-major cells in the owner's approved concept sheets. The retired Poodle
# portrait is deliberately omitted; Goldendoodle has its own source image.
SHEETS = {
    'original-six.png': (3, 2, ['husky', 'golden', 'border', 'dalmatian', 'aussie', 'shiba']),
    'set-a.png': (3, 2, ['frenchie', 'lab', 'shepherd', 'dachshund', None, None]),
    'set-b.png': (3, 2, ['beagle', 'rottweiler', 'gsp', 'bulldog', 'corso', None]),
    'set-c.png': (2, 2, ['cavalier', 'yorkie', 'doberman', 'corgi']),
    'mixed-one.png': (2, 2, ['floppy_mix', 'pointy_mix', 'shaggy_mix', 'patchwork_mix']),
    'mixed-two.png': (2, 2, ['velvet_mix', 'copper_mix', 'silver_mix', 'cream_mix']),
    'goldendoodle.png': (1, 1, ['goldendoodle']),
}
FULL_BODY_HEAD_CROPS = {
    'big_ear_mix', 'sable_saddle_mix', 'spotted_hound_mix',
    'tiny_scruff_mix', 'plush_tail_mix', 'broad_chest_mix',
    'long_nose_mix', 'silky_ear_mix', 'button_ear_mix',
    'snow_patch_mix', 'copper_brindle_mix', 'salt_pepper_mix',
    'tri_flop_mix', 'round_muzzle_mix',
}


def save(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists() or path.read_bytes() != data:
        path.write_bytes(data)


def render(svg: bytes, size: int) -> bytes:
    handle = Rsvg.Handle.new_from_data(svg)
    surface = cairo.ImageSurface(cairo.FORMAT_ARGB32, size, size)
    context = cairo.Context(surface)
    context.scale(size / 108, size / 108)
    handle.render_cairo(context)
    output = io.BytesIO()
    surface.write_to_png(output)
    return output.getvalue()


def portrait_svg(image: bytes, width: int, height: int, columns: int, rows: int,
                 index: int, foreground: bool, background: str) -> bytes:
    cell_w, cell_h = width / columns, height / rows
    assert cell_w == cell_h, (width, height, columns, rows)
    margin = 40 if columns == 3 else 60 if columns == 2 else 80
    cell = cell_w - margin * 2
    scale = 108 / cell
    col, row = index % columns, index // columns
    x = -(col * cell_w + margin) * scale
    y = -(row * cell_h + margin) * scale
    image_width, image_height = width * scale, height * scale
    uri = 'data:image/png;base64,' + base64.b64encode(image).decode('ascii')
    # Keep the full head within both the static square and Android's adaptive mask.
    scale_inset = 0.68 if foreground else 0.80
    inset = (108 - 108 * scale_inset) / 2
    transform = f' transform="translate({inset:.5f} {inset:.5f}) scale({scale_inset:.5f})"'
    backing = '' if foreground else f'<rect width="108" height="108" rx="24" fill="{background}"/>'
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">'
           f'<defs><clipPath id="edge"><rect width="108" height="108" rx="24"/></clipPath>'
           f'<filter id="soft"><feGaussianBlur stdDeviation="5"/></filter>'
           f'<mask id="fade"><rect x="3" y="3" width="102" height="102" rx="20" fill="white" filter="url(#soft)"/></mask></defs>'
           f'{backing}'
           f'<g{transform} clip-path="url(#edge)" mask="url(#fade)">'
           f'<image href="{uri}" x="{x:.5f}" y="{y:.5f}" width="{image_width:.5f}" height="{image_height:.5f}"/>'
           '</g></svg>')
    return svg.encode('utf-8')


def new_portrait(sheet: Image.Image, size: int, background: str | None, head_crop: bool) -> bytes:
    head = sheet.crop((0, 0, 490, 1024//3)).convert('RGBA')
    if head_crop:
        head = head.crop((200, 0, 490, 235))
    alpha = head.getchannel('A').point(lambda value: min(255, max(0, round((value-80)*255/170))))
    if head_crop:
        width, height = head.size
        edge = Image.new('L', (width, height))
        pixels = edge.load()
        for y in range(height):
            for x in range(width):
                pixels[x, y] = round(255*min(1, x/75)*min(1, (height-1-y)/55))
        alpha = ImageChops.multiply(alpha, edge)
    head.putalpha(alpha)
    box = alpha.getbbox()
    assert box is not None
    head = head.crop(box)
    canvas = Image.new('RGBA', (size, size))
    if background is not None:
        draw = ImageDraw.Draw(canvas)
        draw.rounded_rectangle((0, 0, size-1, size-1), radius=round(size*.22), fill=background)
    inset = .94 if head_crop else .80
    portrait = ImageOps.contain(head, (round(size*inset), round(size*inset)), Image.Resampling.LANCZOS)
    canvas.alpha_composite(portrait, ((size-portrait.width)//2, (size-portrait.height)//2))
    output = io.BytesIO()
    canvas.save(output, 'PNG', optimize=True)
    return output.getvalue()


def main() -> None:
    catalog = json.loads((ROOT / 'app/src/main/assets/dog-themes.json').read_text())
    first = {}
    for entry in catalog:
        key = entry['id'].rsplit('_', 1)[0].removeprefix('dog_')
        first.setdefault(key, entry)
    assert len(first) == 80, len(first)
    aliases = []
    for sheet, (columns, rows, breeds) in SHEETS.items():
        image = (SOURCE / sheet).read_bytes()
        pixels = Image.open(io.BytesIO(image)).convert('RGB')
        # Read-only dimensions via PNG header; the source is retained unchanged.
        width = int.from_bytes(image[16:20], 'big')
        height = int.from_bytes(image[20:24], 'big')
        assert width % columns == 0 and height % rows == 0
        for index, breed in enumerate(breeds):
            if breed is None:
                continue
            assert breed in first, breed
            col, row = index % columns, index // columns
            sample = pixels.getpixel((int((col + .86) * width / columns), int((row + .56) * height / rows)))
            background = '#%02x%02x%02x' % sample
            svg = portrait_svg(image, width, height, columns, rows, index, False, background)
            fg = portrait_svg(image, width, height, columns, rows, index, True, background)
            save(ASSETS / f'{breed}.png', render(svg, 144))
            save(RES / 'drawable-nodpi' / f'dog_howl_{breed}_foreground.png', render(fg, 432))
            for density, size in DENSITIES.items():
                save(RES / f'mipmap-{density}' / f'wag_howl_{breed}.png', render(svg, size))
            adaptive = (f'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">'
                        f'<background android:drawable="@color/dog_howl_bg_{breed}"/>'
                        f'<foreground android:drawable="@drawable/dog_howl_{breed}_foreground"/>'
                        '</adaptive-icon>\n').encode()
            for version in ('v26', 'v33'):
                save(RES / f'mipmap-anydpi-{version}' / f'wag_howl_{breed}.xml', adaptive)
            aliases.append((breed, first[breed]['bg']))
    plan = json.loads(PLAN.read_text())
    assert len(plan['newEntries']) == 52
    for entry in plan['newEntries']:
        breed = entry['slug']
        assert breed in first and breed not in {key for key, _ in aliases}
        with Image.open(NEW_SOURCE / f'{breed}.webp') as sheet:
            background = entry['iconBg']
            head_crop = breed in FULL_BODY_HEAD_CROPS
            save(ASSETS / f'{breed}.png', new_portrait(sheet, 144, background, head_crop))
            save(RES / 'drawable-nodpi' / f'dog_howl_{breed}_foreground.png', new_portrait(sheet, 432, None, head_crop))
            for density, size in DENSITIES.items():
                save(RES / f'mipmap-{density}' / f'wag_howl_{breed}.png', new_portrait(sheet, size, background, head_crop))
            adaptive = (f'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">'
                        f'<background android:drawable="@color/dog_howl_bg_{breed}"/>'
                        f'<foreground android:drawable="@drawable/dog_howl_{breed}_foreground"/>'
                        '</adaptive-icon>\n').encode()
            for version in ('v26', 'v33'):
                save(RES / f'mipmap-anydpi-{version}' / f'wag_howl_{breed}.xml', adaptive)
        aliases.append((breed, background))
    assert len(aliases) == 80 and len({key for key, _ in aliases}) == 80
    colors = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    for breed, color in aliases:
        colors.append(f'    <color name="dog_howl_bg_{breed}">{color}</color>')
    colors.append('</resources>')
    save(RES / 'values' / 'dog_howl_colors.xml', ('\n'.join(colors) + '\n').encode())
    block = [BEGIN]
    for breed, _ in aliases:
        block.append(
            f'        <activity-alias android:name="app.daybreak.DogHowl_{breed}" '
            'android:targetActivity="app.daybreak.LauncherActivity" '
            'android:enabled="false" android:exported="true" '
            f'android:icon="@mipmap/wag_howl_{breed}" android:label="@string/app_name">'
            '<intent-filter><action android:name="android.intent.action.MAIN" />'
            '<category android:name="android.intent.category.LAUNCHER" /></intent-filter>'
            '</activity-alias>')
    block.append('        ' + END)
    manifest = MANIFEST.read_text()
    if BEGIN in manifest:
        start, stop = manifest.index(BEGIN), manifest.index(END) + len(END)
        manifest = manifest[:start] + '\n'.join(block) + manifest[stop:]
    else:
        marker = '<!-- BEGIN GENERATED WAGRISE ICON ALIASES -->'
        assert marker in manifest
        manifest = manifest.replace(marker, '\n'.join(block) + '\n        ' + marker, 1)
    ET.fromstring(manifest)
    save(MANIFEST, manifest.encode())
    begin = '// BEGIN GENERATED ADDITIONAL DOG ICON CASES'
    end = '// END GENERATED ADDITIONAL DOG ICON CASES'
    java = APPEARANCE.read_text()
    assert begin in java and end in java
    cases = [begin]
    for entry in plan['newEntries']:
        breed = entry['slug']
        cases.append(f'            case "dog_howl_{breed}":return R.mipmap.wag_howl_{breed};')
    cases.append('            ' + end)
    java = java[:java.index(begin)] + '\n'.join(cases) + java[java.index(end)+len(end):]
    save(APPEARANCE, java.encode())
    print(f'Generated {len(aliases)} breed launcher portraits and aliases.')


if __name__ == '__main__':
    main()
