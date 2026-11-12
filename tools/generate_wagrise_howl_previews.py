#!/usr/bin/env python3
"""Render local, original Wagrise side-profile howling icon concepts."""

from html import escape
from pathlib import Path

import cairo
import gi

gi.require_version("Rsvg", "2.0")
from gi.repository import Rsvg


ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "Work/design/howling-breeds"
OUT.mkdir(parents=True, exist_ok=True)

# The compact profile and open lower jaw are shared so breed cues can be
# compared at the same launcher size. All art is drawn here, not sourced media.
HEAD = "M32 90Q34 73 42 58Q38 50 43 41Q47 34 59 32Q68 25 77 22L83 22Q88 23 87 29Q86 35 77 38L67 43Q62 51 59 59Q58 76 64 90Z"
JAW = "M65 49Q72 49 79 44L82 48Q78 53 68 55Z"
MOUTH = "M67 42 79 39Q75 46 67 48Z"

BREEDS = [
    {
        "id": "husky", "name": "Husky", "note": "Pointed ear, gray and white mask, icy blue howl lines.",
        "bg": "#364047", "coat": "#E8E6DE", "mark": "#87949B", "nose": "#29323A", "sound": "#54C1F4",
        "ear": '<path d="M45 40 45 16Q46 12 50 16L64 33Z" fill="{coat}"/>',
        "details": '<path d="M43 40Q50 36 55 39L53 55 45 61Q38 51 43 40Z" fill="{mark}"/>'
                   '<path d="M52 32Q59 28 65 28L67 43 58 51Q60 40 52 32Z" fill="{mark}"/>',
        "eye": '<circle cx="63" cy="35" r="2.3" fill="{nose}"/>',
    },
    {
        "id": "golden", "name": "Golden Retriever", "note": "Drooping ear, golden fur, and a soft feathery neck.",
        "bg": "#F0D39A", "coat": "#6C471D", "mark": "#9B6628", "nose": "#231A12", "sound": "#6C471D",
        "ear": '<path d="M49 35Q38 35 37 49Q35 58 42 65Q52 64 57 47Z" fill="{mark}"/>',
        "details": '<path d="M45 59Q38 67 34 78L40 75 35 88 48 78 52 88Q51 72 56 61Z" fill="{mark}"/>'
                   '<path d="M64 30Q72 24 80 23L84 25Q80 32 69 36Z" fill="{mark}"/>'
                   '<path d="M43 39Q52 35 55 40Q55 56 43 61Q38 48 43 39Z" fill="{mark}"/>',
        "eye": '<circle cx="63" cy="35" r="2.2" fill="{nose}"/>',
    },
    {
        "id": "border_collie", "name": "Border Collie", "note": "Half-pricked ear and a clear black and white face.",
        "bg": "#191E21", "coat": "#F4F3EC", "mark": "#191E21", "nose": "#191E21", "sound": "#B8B7B1",
        "ear": '<path d="M45 39 45 19Q47 15 51 21L55 24 60 18Q63 27 63 34Z" fill="{coat}"/>'
               '<path d="M45 19Q47 15 51 21L55 24 60 18Q61 22 61 26L54 28Z" fill="{mark}"/>',
        "details": '<path d="M47 35Q53 30 61 31L67 29 68 44 56 51Q59 39 47 35Z" fill="{mark}"/>'
                   '<path d="M47 58Q42 67 39 80L48 74Q46 85 50 90L56 90Q55 70 59 59Z" fill="{mark}"/>',
        "eye": '<circle cx="63" cy="35" r="2.1" fill="{coat}"/>',
    },
    {
        "id": "dalmatian", "name": "Dalmatian", "note": "Folded dark ear and distinct spots on a pale coat.",
        "bg": "#F4F2EB", "coat": "#FFFFFF", "mark": "#242527", "nose": "#242527", "sound": "#66696D",
        "ear": '<path d="M49 34Q40 34 38 45Q36 54 42 59Q50 59 54 47L55 37Z" fill="{mark}"/>',
        "head_stroke": ' stroke="{mark}" stroke-width="2.5" stroke-linejoin="round"',
        "details": '<path d="M43 40Q49 37 54 39L52 53 44 59Q39 48 43 40Z" fill="{mark}"/>'
                   '<circle cx="48" cy="68" r="4.4" fill="{mark}"/>'
                   '<circle cx="55" cy="80" r="3.8" fill="{mark}"/>'
                   '<circle cx="64" cy="33" r="3.2" fill="{mark}"/>',
        "eye": '<circle cx="65" cy="35" r="1.6" fill="{coat}"/>',
    },
    {
        "id": "australian_shepherd", "name": "Australian Shepherd", "note": "Folded ear, blue merle markings, and copper cheek.",
        "bg": "#26384C", "coat": "#ADC8CF", "mark": "#DDA17B", "nose": "#26384C", "sound": "#DDA17B",
        "ear": '<path d="M44 40 44 22Q46 18 53 23L60 29 55 39Z" fill="{coat}"/>'
               '<path d="M44 22Q46 18 53 23L60 29 51 31Z" fill="#566C78"/>',
        "details": '<path d="M43 41Q50 35 57 38L54 56 44 61Q39 50 43 41Z" fill="{mark}"/>'
                   '<path d="M56 32Q62 28 69 27L69 38 60 43Z" fill="#566C78"/>'
                   '<path d="M48 65Q50 60 54 59L53 70 47 73Z" fill="#566C78"/>'
                   '<circle cx="55" cy="80" r="3.5" fill="#566C78"/>',
        "eye": '<circle cx="64" cy="35" r="2.2" fill="{nose}"/>',
    },
]


def svg_for(breed):
    values = {**breed, "head_stroke": breed.get("head_stroke", "")}
    for part in ("ear", "details", "eye", "head_stroke"):
        values[part] = values[part].format(**breed)
    svg = (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" '
        'width="432" height="432" role="img" aria-label="Howling {name} side profile">'
        '<rect width="108" height="108" rx="27" fill="{bg}"/>'
        '{ear}'
        '<path d="' + HEAD + '" fill="{coat}"{head_stroke}/>'
        '{details}'
        '<path d="' + MOUTH + '" fill="{bg}"/>'
        '<path d="' + JAW + '" fill="{coat}"/>'
        '<path d="M77 23Q82 20 87 23L87 29Q84 31 80 30Z" fill="{nose}"/>'
        '{eye}'
        '<path d="M90 21q5-3 7-8M91 29q9-3 12-10" fill="none" '
        'stroke="{sound}" stroke-width="4" stroke-linecap="round"/>'
        '</svg>'
    )
    return svg.format(**values)


def render(svg, path, size):
    sized = svg.replace('width="432" height="432"', f'width="{size}" height="{size}"', 1)
    handle = Rsvg.Handle.new_from_data(sized.encode())
    surface = cairo.ImageSurface(cairo.FORMAT_ARGB32, size, size)
    context = cairo.Context(surface)
    handle.render_cairo(context)
    surface.write_to_png(str(path))


for breed in BREEDS:
    svg = svg_for(breed)
    (OUT / f"{breed['id']}.svg").write_text(svg)
    render(svg, OUT / f"{breed['id']}.png", 432)
    render(svg, OUT / f"{breed['id']}-launcher-size.png", 48)

cards = "\n".join(
    '<article><img class="large" src="howling-breeds/{id}.svg" alt="Howling {name} icon">'
    '<div class="caption"><img class="small" src="howling-breeds/{id}-launcher-size.png" alt="">'
    '<div><h2>{name}</h2><p>{note}</p></div></div></article>'.format(
        id=breed["id"], name=escape(breed["name"]), note=escape(breed["note"])
    )
    for breed in BREEDS
)
page = f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Wagrise howling breed icon concepts</title>
<style>
*{{box-sizing:border-box}}body{{margin:0;background:#101923;color:#F8F5ED;font:16px/1.4 system-ui,sans-serif}}
main{{max-width:1050px;margin:auto;padding:30px 20px 54px}}h1{{font-size:clamp(29px,5vw,40px);letter-spacing:-.04em;margin:0 0 7px}}
.intro{{max-width:720px;color:#C7D2D0;margin:0 0 23px}}.grid{{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:16px}}
article{{background:#1C2B37;border:1px solid #526577;border-radius:22px;padding:17px}}.large{{display:block;width:180px;height:180px;margin:4px auto 18px}}
.caption{{display:flex;gap:12px;align-items:center}}.small{{width:48px;height:48px;flex:none}}h2{{font-size:18px;margin:0 0 4px}}.caption p{{font-size:13px;color:#D2DCE0;margin:0}}
footer{{color:#B8C7CD;font-size:13px;margin-top:24px}}
</style></head><body><main><h1>Wagrise · the howling set</h1>
<p class="intro">Five side-profile breed icons. Each card shows the large drawing and a 48-pixel launcher preview so you can judge whether the breed still reads at phone size.</p>
<div class="grid">{cards}</div><footer>Original vector concepts for review. S2 Quiet Ripples remains the main icon in the local app source. These howling icons have not been added to the app or installed on a phone. <a style="color:#BCE7FF" href="../WAGRISE-HOWL-ICON-DIRECTIONS.html">See the refined six-breed set</a>.</footer>
</main></body></html>'''
(OUT / "FIRST-VECTOR-DRAFT.html").write_text(page)
print("Generated five howling breed icon concepts and launcher-size previews.")
