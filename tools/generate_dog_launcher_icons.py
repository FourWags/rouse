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
from PIL import Image

gi.require_version('Rsvg', '2.0')
from gi.repository import Rsvg

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'tools/dog_howl_source'
RES = ROOT / 'app/src/main/res'
ASSETS = ROOT / 'app/src/main/assets/dog-icons'
MANIFEST = ROOT / 'app/src/main/AndroidManifest.xml'
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


def main() -> None:
    catalog = json.loads((ROOT / 'app/src/main/assets/dog-themes.json').read_text())
    first = {}
    for entry in catalog:
        key = entry['id'].rsplit('_', 1)[0].removeprefix('dog_')
        first.setdefault(key, entry)
    assert len(first) == 28, len(first)
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
    assert len(aliases) == 28 and len({key for key, _ in aliases}) == 28
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
    print(f'Generated {len(aliases)} approved breed launcher portraits and aliases.')


if __name__ == '__main__':
    main()
