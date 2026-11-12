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
DEST = ROOT / 'app/src/main/assets'


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
    out = DEST / 'dog-poses'; out.mkdir(exist_ok=True)
    thumbs = DEST / 'dog-thumbs'; thumbs.mkdir(exist_ok=True)
    catalog_only = sys.argv[1:] == ['--catalog-only']
    for record in records:
        for kind in ('curl', 'yawn'):
            if record['pose'].startswith('coat-expansion/'):
                src = ADDED_POSES / f'{record["pose"].split("/", 1)[1]}-{kind}.png'
            else:
                src = POSES / f'{record["pose"]}-{kind}.png'
            assert src.exists(), src
            pose_file = out / f'{record["id"]}-{kind}.webp'
            thumb_file = thumbs / f'{record["id"]}.webp'
            if catalog_only:
                assert pose_file.exists() and (kind != 'curl' or thumb_file.exists())
            else:
                with Image.open(src) as image:
                    image.save(pose_file, 'WEBP', quality=90, method=6)
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
