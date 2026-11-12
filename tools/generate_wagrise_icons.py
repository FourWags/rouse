#!/usr/bin/env python3
"""Generate theme-aware launcher assets from the approved Wagrise vector concepts.

The SVG source of each selected concept is stored in wagrise_icon_shapes.json so
release source packages do not depend on the interactive design preview files.
"""

import io
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

import cairo
import gi

gi.require_version("Rsvg", "2.0")
from gi.repository import Rsvg  # noqa: E402


ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
ASSETS = ROOT / "app/src/main/assets"
SHAPES = json.loads((ROOT / "tools/wagrise_icon_shapes.json").read_text())["shapes"]
CATALOG_PATH = ASSETS / "appearance.js"
TEMPLATE_PATH = ASSETS / "icon-templates.js"
MANIFEST_PATH = ROOT / "app/src/main/AndroidManifest.xml"
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
BEGIN = "<!-- BEGIN GENERATED WAGRISE ICON ALIASES -->"
END = "<!-- END GENERATED WAGRISE ICON ALIASES -->"


def load_assignment(path, prefix):
    text = path.read_text()
    assert text.startswith(prefix) and text.rstrip().endswith(";")
    return json.loads(text[len(prefix):].strip().removesuffix(";"))


def save_assignment(path, prefix, value):
    path.write_text(prefix + json.dumps(value, ensure_ascii=False, separators=(",", ":")) + ";\n")


def title(value):
    return value[0].upper() + value[1:]


def svg_for(shape, colors, foreground=False, canvas_size=108):
    root = ET.fromstring(shape["svg"])
    root.set("xmlns", "http://www.w3.org/2000/svg")
    root.attrib.pop("viewbox", None)
    root.set("viewBox", "0 0 108 108")
    root.set("width", str(canvas_size))
    root.set("height", str(canvas_size))
    values = {"bg": colors["background"], "main": colors["primary"],
              "soft": colors["secondary"], "light": colors["light"]}
    for element in root.iter():
        classes = element.attrib.pop("class", "").split()
        for class_name in classes:
            if class_name in values:
                element.set("fill", values[class_name])
            if class_name.endswith("-stroke") and class_name[:-7] in values:
                element.set("stroke", values[class_name[:-7]])
    if foreground:
        for child in list(root):
            if child.tag == "rect" and child.get("width") == "108" and child.get("height") == "108":
                root.remove(child)
        group = ET.Element("g", {"transform": "translate(15.12 15.12) scale(0.72)"})
        for child in list(root):
            root.remove(child)
            group.append(child)
        root.append(group)
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def render_png(svg, size):
    handle = Rsvg.Handle.new_from_data(svg)
    surface = cairo.ImageSurface(cairo.FORMAT_ARGB32, size, size)
    context = cairo.Context(surface)
    context.scale(size / 108.0, size / 108.0)
    handle.render_cairo(context)
    output = io.BytesIO()
    surface.write_to_png(output)
    return output.getvalue()


def write_if_changed(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and path.read_bytes() == content:
        return
    path.write_bytes(content)


def generate_resources(themes):
    for theme in themes:
        colors = {"background": theme["background"], "primary": theme["primary"],
                  "secondary": theme["secondary"], "light": theme["preferredText"]}
        for shape in SHAPES:
            name = f"{theme['id']}_{shape['id']}"
            colored = svg_for(shape, colors)
            foreground = svg_for(shape, colors, foreground=True)
            write_if_changed(RES / "drawable-nodpi" / f"icon_foreground_{name}.png", render_png(foreground, 432))
            for density, size in DENSITIES.items():
                write_if_changed(RES / f"mipmap-{density}" / f"ic_launcher_{name}.png", render_png(colored, size))
            adaptive = (f'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">'
                        f'<background android:drawable="@color/icon_bg_{theme["id"]}"/>'
                        f'<foreground android:drawable="@drawable/icon_foreground_{name}"/></adaptive-icon>\n').encode()
            for version in ("v26", "v33"):
                write_if_changed(RES / f"mipmap-anydpi-{version}" / f"ic_launcher_{name}.xml", adaptive)


def update_templates():
    templates = load_assignment(TEMPLATE_PATH, "window.ICON_TEMPLATES=")
    placeholder = {"background": "{{background}}", "primary": "{{primary}}",
                   "secondary": "{{secondary}}", "light": "{{text}}"}
    for shape in SHAPES:
        templates[shape["id"]] = svg_for(shape, placeholder, canvas_size=512).decode()
    save_assignment(TEMPLATE_PATH, "window.ICON_TEMPLATES=", templates)


def update_catalog(catalog):
    old = [icon for icon in catalog["icons"] if not icon["id"].startswith("wr_")]
    selected = ["wr_s2", "wr_p1", "wr_p2", "wr_s1", "wr_s4", "wr_h1", "wr_h3", "wr_h4"]
    lookup = {shape["id"]: shape for shape in SHAPES}
    favorite_icons = [{"id": icon, "name": f"{lookup[icon].get('code', 'S2')} {lookup[icon]['name']}",
                       "group": "Wagrise favorites"} for icon in selected]
    original_icons = [{"id": shape["id"], "name": f"{index:02d} {shape['name']}",
                       "group": "Original Wagrise designs"}
                      for index, shape in enumerate(SHAPES, 1) if shape["group"] == "original"]
    previous_icons = [{**icon, "group": "Earlier icons"} for icon in old]
    catalog["icons"] = favorite_icons + original_icons + previous_icons
    save_assignment(CATALOG_PATH, "window.APPEARANCE=", catalog)


def update_manifest(themes):
    manifest = MANIFEST_PATH.read_text()
    aliases = []
    for theme in themes:
        for shape in SHAPES:
            resource = f"ic_launcher_{theme['id']}_{shape['id']}"
            component = f"app.daybreak.{title(theme['id'])}{title(shape['id'])}Launcher"
            aliases.append(
                f'        <activity-alias android:name="{component}" '
                'android:targetActivity="app.daybreak.LauncherActivity" '
                'android:enabled="false" android:exported="true" '
                f'android:icon="@mipmap/{resource}" android:label="@string/app_name">'
                '<intent-filter><action android:name="android.intent.action.MAIN" />'
                '<category android:name="android.intent.category.LAUNCHER" /></intent-filter>'
                '</activity-alias>')
    block = BEGIN + "\n" + "\n".join(aliases) + "\n        " + END
    if BEGIN in manifest:
        manifest = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END), block, manifest, count=1, flags=re.S)
    else:
        marker = '<activity-alias android:name="${applicationId}.FourWagsIcon_a"'
        assert marker in manifest
        manifest = manifest.replace(marker, block + "\n" + marker, 1)
    MANIFEST_PATH.write_text(manifest)


def main():
    assert len(SHAPES) == 18 and len({shape["id"] for shape in SHAPES}) == 18
    catalog = load_assignment(CATALOG_PATH, "window.APPEARANCE=")
    themes = [theme for theme in catalog["themes"] if not theme["id"].startswith("shiba_")]
    assert len(themes) == 16
    generate_resources(themes)
    update_templates()
    update_catalog(catalog)
    update_manifest(themes)
    print(f"Generated {len(SHAPES)} Wagrise designs for {len(themes)} built-in palettes; S2 is the new-install default.")


if __name__ == "__main__":
    main()
