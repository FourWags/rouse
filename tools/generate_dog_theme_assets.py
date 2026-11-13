"""Build the in-app dog catalog and compact static poses from reviewed concept art."""
from pathlib import Path
import json
import re
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'Work/design/WAGRISE-28-DOG-THEME-REVIEW.html'
POSES = ROOT / 'Work/design/static-poses'
ADDED_POSES = ROOT / 'Work/design/coat-expansion/poses'
COAT_PLAN = ROOT / 'Work/design/coat-expansion/coat-plan.json'
PALETTE_PLAN = ROOT / 'Work/design/coat-expansion/palette-plan.json'
CATALOG_80 = ROOT / 'tools/dog_catalog_80_plan.json'
NEW_ART = ROOT / 'tools/dog_catalog_80_source'
DEST = ROOT / 'app/src/main/assets'

COAT_COLORS = {
    'black': '#292A2C', 'charcoal': '#37373A', 'blue': '#405767',
    'gray': '#777A79', 'grey': '#777A79', 'silver': '#ACAFAB',
    'white': '#EDE9DE', 'cream': '#E6D7BB', 'ivory': '#EAE1CD',
    'fawn': '#BD936B', 'tan': '#A9784F', 'rust': '#A6633F',
    'red': '#A65D3D', 'copper': '#AE6942', 'gold': '#BF8D4F',
    'golden': '#BF8D4F', 'orange': '#C08049', 'apricot': '#C78659',
    'brown': '#76543D', 'chocolate': '#684635', 'cocoa': '#76503A',
    'liver': '#795445', 'sable': '#886749', 'brindle': '#765A47',
    'mahogany': '#824735', 'lemon': '#D7B975', 'oat': '#D2B891',
    'honey': '#B88750', 'sand': '#C5AA7F', 'wheaten': '#CCAA7D',
    'buff': '#D6B88C', 'salt': '#7B807F', 'tricolor': '#4A4039',
    'tri': '#4A4039', 'patches': '#6B645D', 'spotted': '#7B7369',
    'pure': '#EDE9DE', 'bright': '#EDE9DE', 'warm': '#DDD6C5',
    'deep': '#7B4D39', 'light': '#D1B38E', 'dark': '#3D3632',
    'solid': '#313033', 'soft': '#8A6C58',
}

def rgb(value):
    return tuple(int(value[i:i+2], 16) for i in (1, 3, 5))

def color(values):
    return '#%02X%02X%02X' % tuple(round(v) for v in values)

def mix(a, b, fraction):
    return color([x * (1 - fraction) + y * fraction for x, y in zip(rgb(a), rgb(b))])

def luminance(value):
    channels = [(v/255/12.92 if v/255 <= .04045 else ((v/255+.055)/1.055)**2.4) for v in rgb(value)]
    return sum(a*b for a, b in zip(channels, (.2126, .7152, .0722)))

def palette(coat, index):
    words = re.findall(r'[a-z]+', coat.lower())
    base = next((COAT_COLORS[w] for w in words if w in COAT_COLORS), '#8A6955')
    second = next((COAT_COLORS[w] for w in words[1:] if w in COAT_COLORS), '#D5B48E')
    is_light = luminance(base) > .35
    # Small shifts keep closely related natural coat shades distinct.
    base = mix(base, '#FFFFFF' if index == 1 else '#000000', .025 if index == 1 else .025)
    bg = mix(base, '#FFFFFF' if is_light else '#11151C', .42 if is_light else .48)
    action = mix(base, '#1E2530' if is_light else '#F6E8CF', .63 if is_light else .68)
    accent = mix(second, '#65472F' if is_light else '#F0D6AC', .26)
    ink = '#211E1B' if luminance(bg) > .179 else '#FFF7EA'
    return bg, action, accent, ink

def sheet_cell(sheet, row, column):
    width, height = sheet.size
    assert (width, height) == (1536, 1024)
    image = sheet.crop((column*512, row*height//3, (column+1)*512, (row+1)*height//3)).convert('RGBA')
    # Image generation leaves a low-alpha colored halo; retain the opaque art.
    alpha = image.getchannel('A').point(lambda value: min(255, max(0, round((value-80)*255/170))))
    image.putalpha(alpha)
    return image

def valid_image(path):
    try:
        with Image.open(path) as image:
            image.verify()
        return True
    except (OSError, ValueError):
        return False


def main():
    text = SOURCE.read_text()
    body = text.split('const rows=[', 1)[1].split('];\nconst packs=', 1)[0]
    rows = []
    for line in body.splitlines():
        match = re.match(r"\['([^']+)','([^']+)',(?:\d+|null),'[^']+',\d+,'([^']+)','(#[0-9A-Fa-f]{6})','(#[0-9A-Fa-f]{6})','(#[0-9A-Fa-f]{6})','(#[0-9A-Fa-f]{6})'\],?", line.strip())
        if match:
            rows.append(match.groups())
    assert len(rows) == 28, len(rows)
    records = []
    for index, (breed, name, coat, bg, action, accent, ink) in enumerate(rows):
        if breed == 'poodle':
            continue
        letter = 'ABCDEFG'[index // 4]
        row = index % 4
        records.append(dict(id=f'dog_{breed}_{len([r for r in records if r["breed"] == name])}', breed=name, coat=coat, bg=bg, action=action, accent=accent, ink=ink, pose=f'{letter}-{row}'))
    extra = [
        ('husky','Black & white','#25282C','#F5F4ED','#A9C9D9','#FFFFFF','H-0'),
        ('husky','Red & white','#654A43','#F5E8DB','#E1B28E','#FFF8F1','H-1'),
        ('golden','Deep gold','#834E29','#F9E4BE','#DDB477','#FFF7E9','H-2'),
        ('dalmatian','Liver spots','#F5EEE4','#5A3A30','#8D5845','#2C201C','H-3'),
        ('aussie','Red merle','#513936','#EED5C6','#C78365','#FFF7F1','I-0'),
        ('shiba','Black & tan','#1C1B1A','#E2A56A','#D98242','#F6EFE2','I-1'),
        ('shiba','Sesame','#282521','#E0B184','#9A7558','#F6EFE2','I-2'),
        ('lab','Chocolate','#4A362D','#F0DCC0','#C39169','#FFF8ED','I-3'),
        ('lab','Yellow','#E7D2A7','#5C4127','#8C6736','#271D14','J-0'),
    ]
    for breed, coat, bg, action, accent, ink, pose in extra:
        parent = next(r for r in records if r['id'] == f'dog_{breed}_0')
        number = len([r for r in records if r['breed'] == parent['breed']])
        records.append(dict(id=f'dog_{breed}_{number}', breed=parent['breed'], coat=coat, bg=bg, action=action, accent=accent, ink=ink, pose=pose))
    assert len(records) == 36
    new_coats = json.loads(COAT_PLAN.read_text())
    new_colors = json.loads(PALETTE_PLAN.read_text())
    assert set(new_coats) == set(new_colors)
    for breed_key, variants in new_coats.items():
        parent = next((record for record in records if record['id'] == f'{breed_key}_0'), None)
        breed_name = parent['breed'] if parent else 'Goldendoodle' if breed_key == 'dog_goldendoodle' else None
        assert breed_name and len(variants) == len(new_colors[breed_key])
        for row, ((coat, _description), (bg, action, accent, ink)) in enumerate(zip(variants, new_colors[breed_key])):
            number = len([record for record in records if record['breed'] == breed_name])
            records.append(dict(id=f'{breed_key}_{number}', breed=breed_name, coat=coat,
                                bg=bg, action=action, accent=accent, ink=ink,
                                pose=f'coat-expansion/{breed_key}-{row}'))
    assert len(records) == 84
    plan = json.loads(CATALOG_80.read_text())
    assert plan['targetNamedDogs'] == 50 and plan['targetMuttStyles'] == 30
    assert len(plan['newEntries']) == 52
    assert len({entry['slug'] for entry in plan['newEntries']}) == 52
    for entry in plan['newEntries']:
        slug, name = entry['slug'], entry['name']
        assert slug and re.fullmatch(r'[a-z_]+', slug)
        assert len(entry['coats']) == 3
        assert not any(r['id'].startswith(f'dog_{slug}_') for r in records)
        assert (NEW_ART / f'{slug}.webp').exists(), slug
        for row, coat in enumerate(entry['coats']):
            bg, action, accent, ink = palette(coat, row)
            records.append(dict(id=f'dog_{slug}_{row}', breed=name, kind=entry['kind'], coat=coat,
                                bg=bg, action=action, accent=accent, ink=ink,
                                pose=f'catalog-80/{slug}-{row}'))
    for record in records:
        record.setdefault('kind', 'mix' if record['breed'].lower().endswith(' mix') else 'breed')
    assert len(records) == 240
    assert len({r['id'] for r in records}) == 240
    assert len({r['breed'] for r in records}) == 80
    assert len({r['breed'] for r in records if r['kind'] == 'breed'}) == 50
    assert len({r['breed'] for r in records if r['kind'] == 'mix'}) == 30
    out = DEST / 'dog-poses'; out.mkdir(exist_ok=True)
    thumbs = DEST / 'dog-thumbs'; thumbs.mkdir(exist_ok=True)
    catalog_only = sys.argv[1:] == ['--catalog-only']
    for record in records:
        for kind in ('curl', 'yawn'):
            if record['pose'].startswith('coat-expansion/'):
                src = ADDED_POSES / f'{record["pose"].split("/", 1)[1]}-{kind}.png'
            elif record['pose'].startswith('catalog-80/'):
                slug, row = record['pose'].split('/', 1)[1].rsplit('-', 1)
                src = NEW_ART / f'{slug}.webp'
            else:
                src = POSES / f'{record["pose"]}-{kind}.png'
            assert src.exists(), src
            pose_file = out / f'{record["id"]}-{kind}.webp'
            thumb_file = thumbs / f'{record["id"]}.webp'
            if catalog_only:
                assert pose_file.exists() and (kind != 'curl' or thumb_file.exists())
            elif not record['pose'].startswith('catalog-80/') and valid_image(pose_file) and (kind != 'curl' or valid_image(thumb_file)):
                continue
            else:
                with Image.open(src) as original:
                    image = sheet_cell(original, int(row), 1 if kind == 'curl' else 2) if record['pose'].startswith('catalog-80/') else original.copy()
                    image.save(pose_file, 'WEBP', quality=90, method=4)
                    if kind == 'curl':
                        preview=image.copy();preview.thumbnail((112, 92), Image.Resampling.LANCZOS)
                        preview.save(thumb_file,'WEBP',quality=85,method=4)
    data = json.dumps(records, ensure_ascii=False, separators=(',', ':'))
    (DEST / 'dog-themes.json').write_text(data + '\n')
    (DEST / 'dog-themes.js').write_text('window.DOG_THEMES=' + data + ';\n')
    coats = ['/* Generated offline coat swatches; no inline styles are needed in the app. */']
    for record in records:
        coats.append(f'.coat-swatch[data-coat="{record["id"]}"]{{--coat-bg:{record["bg"]};--coat-action:{record["action"]};--coat-accent:{record["accent"]}}}')
    (DEST / 'dog-coats.css').write_text('\n'.join(coats) + '\n')
    print(f'PASS: {len(records)} themes and {len(records)*2} poses')

if __name__ == '__main__': main()
