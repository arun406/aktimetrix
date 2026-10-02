"""Assembles the documentation site's pages into target/site-src, for MkDocs.

The repository's Markdown stays the single source: the README becomes the home page, docs/ the guides, and the
changelog and release notes their pages. Links written for browsing the repository on GitHub are rewritten for the
site; links to other files of the repository point to them on GitHub. The Java API (Javadoc) and the REST API
(OpenAPI) references are added when they have been generated:

    ./mvnw -B install -DskipTests && ./mvnw -B -pl aktimetrix-rest test -Dtest=OpenApiTest
    ./mvnw -B javadoc:aggregate
    python3 site/prepare.py && mkdocs build
"""
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "target" / "site-src"
GITHUB = "https://github.com/arun406/aktimetrix/blob/main/"

PAGES = {"README.md": "index.md", "CHANGELOG.md": "changelog.md", "RELEASING.md": "releasing.md"}
PAGES.update({f"docs/{p.name}": p.name for p in sorted((ROOT / "docs").glob("*.md"))})


def target(link, source):
    """Where a repository-relative link points on the site."""
    path, _, anchor = link.partition("#")
    resolved = (ROOT / Path(source).parent / path).resolve()
    relative = resolved.relative_to(ROOT).as_posix() if resolved.is_relative_to(ROOT) else None
    if relative in PAGES:
        page = PAGES[relative]
    elif relative is not None and relative.startswith("img/"):
        page = relative
    else:
        page = GITHUB + (relative or path)
    return page + ("#" + anchor if anchor else "")


def rewrite(text, source):
    def markdown_link(match):
        link = match.group(2)
        if re.match(r"^[a-z]+:|^#", link):
            return match.group(0)
        return match.group(1) + target(link, source) + ")"

    text = re.sub(r"(\]\()([^)\s]+)\)", markdown_link, text)
    # links in HTML: href="./docs/white-paper.md"
    text = re.sub(r'href="(?![a-z]+:|#)([^"]+)"', lambda m: 'href="' + target(m.group(1), source) + '"', text)
    text = text.replace("Back to README", "Back to the home page")
    # images in HTML: src="./img/x.svg", src="../img/x.svg"
    text = re.sub(r'src="(?:\.\./|\./)img/', 'src="img/', text)
    return text


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    OUT.mkdir(parents=True)
    for source, page in PAGES.items():
        (OUT / page).write_text(rewrite((ROOT / source).read_text(encoding="utf-8"), source), encoding="utf-8")
    shutil.copytree(ROOT / "img", OUT / "img")
    shutil.copytree(ROOT / "site" / "rest-api", OUT / "rest-api")
    openapi = ROOT / "aktimetrix-rest" / "target" / "openapi" / "aktimetrix.json"
    if openapi.exists():
        shutil.copy(openapi, OUT / "rest-api" / "aktimetrix.json")
    else:
        print("No OpenAPI document: run OpenApiTest of aktimetrix-rest to add the REST API reference")
    javadoc = ROOT / "target" / "reports" / "apidocs"
    if javadoc.exists():
        shutil.copytree(javadoc, OUT / "api")
    else:
        (OUT / "api").mkdir()
        (OUT / "api" / "index.html").write_text("<p>Run <code>./mvnw javadoc:aggregate</code> to generate the Java API.</p>")
        print("No Javadoc: run ./mvnw javadoc:aggregate to add the Java API reference")
    print(f"Pages assembled in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
