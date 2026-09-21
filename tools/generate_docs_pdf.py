from __future__ import annotations

import re
import sys
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    CondPageBreak,
    Flowable,
    HRFlowable,
    KeepTogether,
    ListFlowable,
    ListItem,
    PageBreak,
    Paragraph,
    Preformatted,
    Spacer,
    SimpleDocTemplate,
    Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents


ROOT = Path(__file__).resolve().parents[1]
FONT_DIR = Path(r"C:\Windows\Fonts")
FONT_REGULAR = FONT_DIR / "msyh.ttc"
FONT_BOLD = FONT_DIR / "msyhbd.ttc"

PAGE_WIDTH, PAGE_HEIGHT = A4
MARGIN_X = 18 * mm
MARGIN_TOP = 18 * mm
MARGIN_BOTTOM = 18 * mm
CONTENT_WIDTH = PAGE_WIDTH - MARGIN_X * 2

NAVY = colors.HexColor("#15324B")
BLUE = colors.HexColor("#1E5A8A")
TEAL = colors.HexColor("#137C8B")
INK = colors.HexColor("#1F2933")
MUTED = colors.HexColor("#5F6B76")
LINE = colors.HexColor("#D5DEE5")
PANEL = colors.HexColor("#F2F6F8")
PANEL_ALT = colors.HexColor("#E8F0F3")
CODE_BG = colors.HexColor("#F4F7F9")


def register_fonts() -> None:
    if not FONT_REGULAR.exists() or not FONT_BOLD.exists():
        raise FileNotFoundError("Microsoft YaHei fonts were not found in C:\\Windows\\Fonts")
    pdfmetrics.registerFont(TTFont("MicrosoftYaHei", str(FONT_REGULAR), subfontIndex=0))
    pdfmetrics.registerFont(TTFont("MicrosoftYaHei-Bold", str(FONT_BOLD), subfontIndex=0))
    pdfmetrics.registerFontFamily(
        "MicrosoftYaHei",
        normal="MicrosoftYaHei",
        bold="MicrosoftYaHei-Bold",
        italic="MicrosoftYaHei",
        boldItalic="MicrosoftYaHei-Bold",
    )


def build_styles() -> dict[str, ParagraphStyle]:
    base = getSampleStyleSheet()
    styles = {
        "Body": ParagraphStyle(
            "Body",
            parent=base["BodyText"],
            fontName="MicrosoftYaHei",
            fontSize=9.2,
            leading=14.2,
            textColor=INK,
            spaceAfter=5,
            wordWrap="CJK",
            alignment=TA_LEFT,
        ),
        "CoverTitle": ParagraphStyle(
            "CoverTitle",
            parent=base["Title"],
            fontName="MicrosoftYaHei-Bold",
            fontSize=28,
            leading=38,
            textColor=NAVY,
            alignment=TA_CENTER,
        ),
        "CoverSubtitle": ParagraphStyle(
            "CoverSubtitle",
            parent=base["BodyText"],
            fontName="MicrosoftYaHei",
            fontSize=13,
            leading=20,
            textColor=BLUE,
            alignment=TA_CENTER,
        ),
        "H2": ParagraphStyle(
            "H2",
            parent=base["Heading1"],
            fontName="MicrosoftYaHei-Bold",
            fontSize=16,
            leading=23,
            textColor=NAVY,
            spaceBefore=10,
            spaceAfter=7,
            keepWithNext=True,
        ),
        "TOCTitle": ParagraphStyle(
            "TOCTitle",
            parent=base["Heading1"],
            fontName="MicrosoftYaHei-Bold",
            fontSize=16,
            leading=23,
            textColor=NAVY,
            spaceBefore=10,
            spaceAfter=7,
            keepWithNext=True,
        ),
        "H3": ParagraphStyle(
            "H3",
            parent=base["Heading2"],
            fontName="MicrosoftYaHei-Bold",
            fontSize=11.8,
            leading=18,
            textColor=BLUE,
            spaceBefore=8,
            spaceAfter=4,
            keepWithNext=True,
        ),
        "H4": ParagraphStyle(
            "H4",
            parent=base["Heading3"],
            fontName="MicrosoftYaHei-Bold",
            fontSize=10.2,
            leading=15,
            textColor=TEAL,
            spaceBefore=6,
            spaceAfter=3,
            keepWithNext=True,
        ),
        "Bullet": ParagraphStyle(
            "Bullet",
            parent=base["BodyText"],
            fontName="MicrosoftYaHei",
            fontSize=9.2,
            leading=13.8,
            textColor=INK,
            wordWrap="CJK",
            spaceAfter=2,
        ),
        "Quote": ParagraphStyle(
            "Quote",
            parent=base["BodyText"],
            fontName="MicrosoftYaHei",
            fontSize=9,
            leading=13.6,
            textColor=MUTED,
            leftIndent=8,
            rightIndent=8,
            borderColor=BLUE,
            borderWidth=0,
            borderPadding=6,
            backColor=PANEL,
            wordWrap="CJK",
            spaceAfter=6,
        ),
        "Code": ParagraphStyle(
            "Code",
            parent=base["Code"],
            fontName="MicrosoftYaHei",
            fontSize=7.8,
            leading=11.2,
            textColor=colors.HexColor("#243B53"),
            backColor=CODE_BG,
            borderColor=LINE,
            borderWidth=0.4,
            borderPadding=6,
            wordWrap="CJK",
            spaceBefore=2,
            spaceAfter=6,
        ),
        "Table": ParagraphStyle(
            "Table",
            parent=base["BodyText"],
            fontName="MicrosoftYaHei",
            fontSize=7.2,
            leading=10.2,
            textColor=INK,
            wordWrap="CJK",
        ),
        "TableHeader": ParagraphStyle(
            "TableHeader",
            parent=base["BodyText"],
            fontName="MicrosoftYaHei-Bold",
            fontSize=7.3,
            leading=10.4,
            textColor=colors.white,
            wordWrap="CJK",
        ),
        "TOC0": ParagraphStyle(
            "TOC0",
            fontName="MicrosoftYaHei-Bold",
            fontSize=8.9,
            leading=11.8,
            textColor=NAVY,
            leftIndent=0,
            firstLineIndent=0,
            spaceBefore=0,
        ),
        "TOC1": ParagraphStyle(
            "TOC1",
            fontName="MicrosoftYaHei",
            fontSize=7.7,
            leading=10.0,
            textColor=MUTED,
            leftIndent=12,
            firstLineIndent=0,
            spaceBefore=0,
        ),
        "TOC2": ParagraphStyle(
            "TOC2",
            fontName="MicrosoftYaHei",
            fontSize=7.4,
            leading=9.4,
            textColor=MUTED,
            leftIndent=24,
            firstLineIndent=0,
            spaceBefore=0,
        ),
    }
    return styles


class SectionRule(Flowable):
    def __init__(self, color: colors.Color = LINE, thickness: float = 0.7):
        super().__init__()
        self.color = color
        self.thickness = thickness
        self.width = CONTENT_WIDTH
        self.height = self.thickness

    def draw(self) -> None:
        self.canv.setStrokeColor(self.color)
        self.canv.setLineWidth(self.thickness)
        self.canv.line(0, 0, self.width, 0)


class SpecDocTemplate(SimpleDocTemplate):
    def __init__(self, filename: str, title: str, **kwargs):
        self.doc_title = title
        self._outline_index = 0
        super().__init__(
            filename,
            pagesize=A4,
            leftMargin=MARGIN_X,
            rightMargin=MARGIN_X,
            topMargin=MARGIN_TOP,
            bottomMargin=MARGIN_BOTTOM,
            title=title,
            author="RAG KnowledgeNexus",
            subject="System design and REST API documentation",
            **kwargs,
        )

    def afterFlowable(self, flowable: Flowable) -> None:
        if not isinstance(flowable, Paragraph):
            return
        style_name = flowable.style.name
        if style_name not in {"H2", "H3", "H4"}:
            return
        level = {"H2": 0, "H3": 1, "H4": 2}[style_name]
        text = flowable.getPlainText()
        key = f"section-{self._outline_index}"
        self._outline_index += 1
        self.canv.bookmarkPage(key)
        self.canv.addOutlineEntry(text, key, level=level, closed=level > 0)
        self.notify("TOCEntry", (level, text, self.page))


def normalize_text(text: str) -> str:
    replacements = {
        "\u2011": "-",
        "\u2013": "-",
        "\u2014": "-",
        "\u2212": "-",
        "\u2192": "->",
        "\u00a0": " ",
    }
    for old, new in replacements.items():
        text = text.replace(old, new)
    return text


def xml_escape(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def inline_markup(text: str) -> str:
    text = normalize_text(text)
    text = xml_escape(text)
    text = re.sub(
        r"`([^`]+)`",
        r'<font name="MicrosoftYaHei" color="#0F4C5C">\1</font>',
        text,
    )
    text = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)
    return text


def is_special_line(line: str) -> bool:
    stripped = line.strip()
    return (
        not stripped
        or stripped.startswith("#")
        or stripped.startswith("```")
        or stripped.startswith("|")
        or stripped.startswith("> ")
        or stripped == "---"
        or stripped.startswith("- ")
        or bool(re.match(r"^\d+\.\s+", stripped))
    )


def table_column_widths(rows: list[list[str]]) -> list[float]:
    if not rows:
        return [CONTENT_WIDTH]
    column_count = max(len(row) for row in rows)
    weights: list[float] = []
    for col in range(column_count):
        values = [row[col] if col < len(row) else "" for row in rows]
        max_len = max((len(value) for value in values), default=1)
        weights.append(max(6, min(38, max_len)) ** 0.72)
    total = sum(weights)
    return [CONTENT_WIDTH * weight / total for weight in weights]


def create_table(rows: list[list[str]], styles: dict[str, ParagraphStyle]) -> Table:
    table_data: list[list[Paragraph]] = []
    for row_idx, row in enumerate(rows):
        style = styles["TableHeader"] if row_idx == 0 else styles["Table"]
        table_data.append([Paragraph(inline_markup(cell.strip()), style) for cell in row])
    table = Table(
        table_data,
        colWidths=table_column_widths(rows),
        repeatRows=1,
        hAlign="LEFT",
        splitByRow=1,
    )
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), NAVY),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("GRID", (0, 0), (-1, -1), 0.35, LINE),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 3.2),
                ("RIGHTPADDING", (0, 0), (-1, -1), 3.2),
                ("TOPPADDING", (0, 0), (-1, -1), 3.2),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3.2),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PANEL]),
            ]
        )
    )
    return table


def parse_table(lines: list[str], styles: dict[str, ParagraphStyle], flowables: list[Flowable]) -> None:
    parsed: list[list[str]] = []
    for line in lines:
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if cells and all(re.fullmatch(r":?-{3,}:?", cell.replace(" ", "")) for cell in cells):
            continue
        parsed.append(cells)
    if parsed:
        flowables.append(Spacer(1, 2))
        flowables.append(create_table(parsed, styles))
        flowables.append(Spacer(1, 6))


def parse_markdown(markdown_text: str, styles: dict[str, ParagraphStyle]) -> list[Flowable]:
    lines = markdown_text.splitlines()
    start_index = next(
        (idx for idx, line in enumerate(lines) if line.startswith("## ")),
        0,
    )
    lines = lines[start_index:]
    flowables: list[Flowable] = []
    paragraph_buffer: list[str] = []
    index = 0

    def flush_paragraph() -> None:
        if not paragraph_buffer:
            return
        text = " ".join(item.strip() for item in paragraph_buffer)
        flowables.append(Paragraph(inline_markup(text), styles["Body"]))
        paragraph_buffer.clear()

    while index < len(lines):
        raw_line = lines[index]
        stripped = raw_line.strip()

        if not stripped:
            flush_paragraph()
            index += 1
            continue

        if stripped.startswith("```"):
            flush_paragraph()
            index += 1
            code_lines: list[str] = []
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code_lines.append(normalize_text(lines[index]))
                index += 1
            if index < len(lines):
                index += 1
            flowables.append(Preformatted("\n".join(code_lines), styles["Code"]))
            continue

        heading_match = re.match(r"^(#{2,4})\s+(.*)$", stripped)
        if heading_match:
            flush_paragraph()
            level = len(heading_match.group(1))
            style_name = {2: "H2", 3: "H3", 4: "H4"}[level]
            flowables.append(CondPageBreak(32 if level == 2 else 22))
            flowables.append(Paragraph(inline_markup(heading_match.group(2)), styles[style_name]))
            if level == 2:
                flowables.append(SectionRule())
                flowables.append(Spacer(1, 3))
            index += 1
            continue

        if stripped == "---":
            flush_paragraph()
            flowables.append(Spacer(1, 4))
            flowables.append(HRFlowable(width="100%", thickness=0.5, color=LINE))
            flowables.append(Spacer(1, 4))
            index += 1
            continue

        if stripped.startswith("|"):
            flush_paragraph()
            table_lines: list[str] = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_lines.append(lines[index].strip())
                index += 1
            parse_table(table_lines, styles, flowables)
            continue

        if stripped.startswith("> "):
            flush_paragraph()
            quote_lines: list[str] = []
            while index < len(lines) and lines[index].strip().startswith("> "):
                quote_lines.append(lines[index].strip()[2:].strip())
                index += 1
            flowables.append(Paragraph(inline_markup(" ".join(quote_lines)), styles["Quote"]))
            continue

        bullet_match = re.match(r"^[-*]\s+(.*)$", stripped)
        ordered_match = re.match(r"^(\d+)\.\s+(.*)$", stripped)
        if bullet_match or ordered_match:
            flush_paragraph()
            ordered = ordered_match is not None
            list_items: list[ListItem] = []
            start_number = int(ordered_match.group(1)) if ordered_match else 1
            while index < len(lines):
                item_line = lines[index].strip()
                if ordered:
                    match = re.match(r"^\d+\.\s+(.*)$", item_line)
                else:
                    match = re.match(r"^[-*]\s+(.*)$", item_line)
                if not match:
                    break
                item_text = match.group(1)
                list_items.append(
                    ListItem(
                        Paragraph(inline_markup(item_text), styles["Bullet"]),
                        leftIndent=10,
                    )
                )
                index += 1
            flowables.append(
                ListFlowable(
                    list_items,
                    bulletType="1" if ordered else "bullet",
                    start=start_number if ordered else None,
                    leftIndent=14,
                    bulletFontName="MicrosoftYaHei",
                    bulletFontSize=8.5,
                    bulletColor=BLUE,
                    spaceAfter=4,
                )
            )
            continue

        paragraph_buffer.append(raw_line)
        index += 1

    flush_paragraph()
    return flowables


def cover_flowables(
    title: str,
    subtitle: str,
    version: str,
    date_text: str,
    phase: str,
    status_text: str,
) -> list[Flowable]:
    meta_style = ParagraphStyle(
        "CoverMeta",
        fontName="MicrosoftYaHei",
        fontSize=9,
        leading=13,
        textColor=INK,
    )
    meta_label_style = ParagraphStyle(
        "CoverMetaLabel",
        parent=meta_style,
        fontName="MicrosoftYaHei-Bold",
        textColor=MUTED,
    )
    return [
        Spacer(1, 62 * mm),
        Paragraph(xml_escape(title), ParagraphStyle(
            "CoverTitleInline",
            fontName="MicrosoftYaHei-Bold",
            fontSize=27,
            leading=37,
            textColor=NAVY,
            alignment=TA_CENTER,
        )),
        Spacer(1, 5 * mm),
        Paragraph(xml_escape(subtitle), ParagraphStyle(
            "CoverSubtitleInline",
            fontName="MicrosoftYaHei",
            fontSize=13,
            leading=20,
            textColor=BLUE,
            alignment=TA_CENTER,
        )),
        Spacer(1, 12 * mm),
        HRFlowable(width="38%", thickness=1.1, color=TEAL, hAlign="CENTER"),
        Spacer(1, 13 * mm),
        Table(
            [
                [Paragraph("版本", meta_label_style), Paragraph(version, meta_style)],
                [Paragraph("日期", meta_label_style), Paragraph(date_text, meta_style)],
                [Paragraph("阶段", meta_label_style), Paragraph(phase, meta_style)],
                [Paragraph("状态", meta_label_style), Paragraph(status_text, meta_style)],
            ],
            colWidths=[24 * mm, 72 * mm],
            hAlign="CENTER",
            style=TableStyle(
                [
                    ("FONTNAME", (0, 0), (-1, -1), "MicrosoftYaHei"),
                    ("FONTSIZE", (0, 0), (-1, -1), 9),
                    ("TEXTCOLOR", (0, 0), (-1, -1), INK),
                    ("TEXTCOLOR", (0, 0), (0, -1), MUTED),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
                    ("TOPPADDING", (0, 0), (-1, -1), 7),
                    ("LINEBELOW", (0, 0), (-1, -2), 0.3, LINE),
                ]
            ),
        ),
    ]


def draw_cover(canvas, doc: SimpleDocTemplate) -> None:
    canvas.saveState()
    canvas.setFillColor(NAVY)
    canvas.rect(0, PAGE_HEIGHT - 8 * mm, PAGE_WIDTH, 8 * mm, stroke=0, fill=1)
    canvas.setFillColor(TEAL)
    canvas.rect(0, PAGE_HEIGHT - 10 * mm, PAGE_WIDTH * 0.34, 2 * mm, stroke=0, fill=1)
    canvas.setFont("MicrosoftYaHei", 8)
    canvas.setFillColor(MUTED)
    canvas.drawCentredString(PAGE_WIDTH / 2, 17 * mm, "RAG KnowledgeNexus | Engineering specification")
    canvas.restoreState()


def draw_content_page(canvas, doc: SpecDocTemplate) -> None:
    canvas.saveState()
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.4)
    canvas.line(MARGIN_X, PAGE_HEIGHT - 13 * mm, PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 13 * mm)
    canvas.setFont("MicrosoftYaHei", 7)
    canvas.setFillColor(MUTED)
    canvas.drawString(MARGIN_X, PAGE_HEIGHT - 10 * mm, doc.doc_title)
    canvas.drawRightString(PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 10 * mm, "RAG KnowledgeNexus")
    canvas.line(MARGIN_X, 13 * mm, PAGE_WIDTH - MARGIN_X, 13 * mm)
    canvas.setFont("MicrosoftYaHei", 7.5)
    canvas.drawRightString(PAGE_WIDTH - MARGIN_X, 8 * mm, f"第 {doc.page} 页")
    canvas.restoreState()


def create_toc(styles: dict[str, ParagraphStyle]) -> TableOfContents:
    toc = TableOfContents()
    toc.levelStyles = [styles["TOC0"], styles["TOC1"], styles["TOC2"]]
    toc.dotsMinLevel = 0
    return toc


def render_document(
    source: Path,
    output: Path,
    title: str,
    subtitle: str,
    version: str,
    date_text: str,
    phase: str,
    status_text: str,
) -> None:
    styles = build_styles()
    markdown_text = normalize_text(source.read_text(encoding="utf-8"))
    output.parent.mkdir(parents=True, exist_ok=True)

    story: list[Flowable] = []
    story.extend(cover_flowables(title, subtitle, version, date_text, phase, status_text))
    story.append(PageBreak())
    story.append(Paragraph("目录", styles["TOCTitle"]))
    story.append(SectionRule())
    story.append(Spacer(1, 5))
    story.append(create_toc(styles))
    story.append(PageBreak())
    story.extend(parse_markdown(markdown_text, styles))

    doc = SpecDocTemplate(str(output), title)
    doc.multiBuild(story, onFirstPage=draw_cover, onLaterPages=draw_content_page)
    print(f"Generated: {output}")


def main() -> int:
    register_fonts()
    jobs = [
        {
            "source": ROOT / "docs" / "requirements" / "RAG-KnowledgeNexus-需求规格说明书.md",
            "output": ROOT / "output" / "pdf" / "RAG-KnowledgeNexus-需求规格说明书.pdf",
            "title": "RAG KnowledgeNexus 需求规格说明书",
            "subtitle": "用户角色 | 功能需求 | 页面与数据 | 非功能需求 | 验收标准",
            "version": "v1.0",
            "date_text": "2026-09-21",
            "phase": "阶段 2 需求分析",
            "status_text": "需求评审稿",
        },
        {
            "source": ROOT / "docs" / "design" / "RAG-KnowledgeNexus-软件设计说明书.md",
            "output": ROOT / "output" / "pdf" / "RAG-KnowledgeNexus-软件设计说明书.pdf",
            "title": "RAG KnowledgeNexus 软件设计说明书",
            "subtitle": "概要设计 | 模块交互 | 详细设计 | 数据结构 | 接口与部署",
            "version": "v1.0",
            "date_text": "2026-09-21",
            "phase": "阶段 3 软件设计",
            "status_text": "设计评审稿",
        },
    ]

    for job in jobs:
        render_document(**job)
    return 0


if __name__ == "__main__":
    sys.exit(main())
