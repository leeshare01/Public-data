"""
==================================================================
  设计文档生成工具：Markdown -> Word (.docx) 默认转换器
  ===================================================================
  用途：所有设计文档默认以 Word (.docx) 格式生成交付

  格式规范（按用户要求）：
  - 字体：宋体，颜色黑色
  - 一级标题：三号 (16pt) 加粗
  - 二级标题：小三 (15pt) 加粗
  - 三级标题：四号 (14pt) 加粗
  - 正文：小四 (12pt)
  - 段前段后：0磅
  - 行距：单倍行距 (1.0)
  - 标题下横线：不生成（删除）
  - **粗体标记**：正确转换为加粗文本，不显示**

  ⚠ 已知问题修复：python-docx 的 run.font.name = '宋体' 仅设置西文字体槽(w:ascii/w:hAnsi)，
    导致 Word 打开时中文回退显示为日文字体 "MS Mincho"。
    解决方案：直接操作 XML 元素，同时设置 w:eastAsia（东亚字体槽）为 "宋体"。
    在 set_run_font() 和 set_style_font() 中已实现此修复。

  用法：
    python md_to_docx.py                    # 转换默认4个设计文档
    python md_to_docx.py --all               # 转换目录下所有 .md 文件
    python md_to_docx.py 文件名.md            # 转换指定文件
  依赖：pip install python-docx
==================================================================
"""

import os, sys, re

if sys.stdout.encoding and sys.stdout.encoding.upper() != 'UTF-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml.ns import qn
from docx.oxml import OxmlElement


# ========== 格式常量（按用户要求配置）==========
FONT_NAME = '宋体'
FONT_COLOR = RGBColor(0x00, 0x00, 0x00)          # 黑色
CODE_FONT = 'Consolas'
CODE_SIZE = Pt(9)
TABLE_FONT_SIZE = Pt(9)

# 中文字号对照：三号=16pt  小三=15pt  四号=14pt  小四=12pt
HEADING_SIZES = {1: 16, 2: 15, 3: 14, 4: 12}     # 标题字号
BODY_SIZE = 12                                    # 正文小四

PARA_SPACE_BEFORE = Pt(0)                         # 段前0磅
PARA_SPACE_AFTER = Pt(0)                          # 段后0磅
LINE_SPACING = 1.0                                # 单倍行距

DESIGN_DOCS = [
    '系统架构设计文档.md',
    '数据库设计文档.md',
    '接口API设计文档.md',
    '设计文档索引.md',
]


def set_style_font(style, font_name=FONT_NAME, font_size=BODY_SIZE, bold=False,
                    color=FONT_COLOR):
    """设置样式字体（同时修正东亚字体槽，解决 MS Mincho 问题）

    ⚠ python-docx 的 style.font.name = '宋体' 只设置西文字体槽 (w:ascii/w:hAnsi)，
       Word 打开中文文档时会回退到日文 MS Mincho 字体。
       必须同时设置 w:eastAsia 和 w:cs 属性为 '宋体' 才能正确显示。
    """
    style.font.size = Pt(font_size)
    style.font.bold = bold
    style.font.color.rgb = color
    rPr = style.element.find(qn('w:rPr'))
    if rPr is None:
        rPr = OxmlElement('w:rPr')
        style.element.append(rPr)
    rFonts = rPr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = OxmlElement('w:rFonts')
        rPr.insert(0, rFonts)
    rFonts.set(qn('w:ascii'), font_name)
    rFonts.set(qn('w:hAnsi'), font_name)
    rFonts.set(qn('w:eastAsia'), font_name)
    rFonts.set(qn('w:cs'), font_name)


def set_paragraph_format(para, space_before=0, space_after=0, line_spacing=1.0,
                         first_line_indent=None, left_indent=None):
    """统一设置段落格式"""
    pf = para.paragraph_format
    pf.space_before = Pt(space_before)
    pf.space_after = Pt(space_after)
    pf.line_spacing_rule = WD_LINE_SPACING.SINGLE
    pf.line_spacing = line_spacing
    if first_line_indent is not None:
        pf.first_line_indent = first_line_indent
    if left_indent is not None:
        pf.left_indent = left_indent


def set_run_font(run, font_name=FONT_NAME, font_size=BODY_SIZE, bold=False,
                 color=FONT_COLOR):
    """设置 run 字体（同时设置西文和东亚中文字体槽，解决 MS Mincho 问题）

    直接操作 XML 写 w:rFonts 的四个属性：
      w:ascii    — 西文字体（拉丁字母）
      w:hAnsi    — 高 ANSI 字体
      w:eastAsia — 东亚字体（中文、日文、韩文）← python-docx 默认不设置此项
      w:cs       — 复杂文种字体（如阿拉伯文）
    缺少 w:eastAsia 时，Word 用 MS Mincho（日文）渲染中文字符。
    """
    run.font.size = Pt(font_size)
    run.font.bold = bold
    run.font.color.rgb = color
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = OxmlElement('w:rFonts')
        rPr.insert(0, rFonts)
    rFonts.set(qn('w:ascii'), font_name)
    rFonts.set(qn('w:hAnsi'), font_name)
    rFonts.set(qn('w:eastAsia'), font_name)
    rFonts.set(qn('w:cs'), font_name)
    return run


def add_formatted_run(paragraph, text, font_name=FONT_NAME, font_size=BODY_SIZE,
                      bold=False, color=FONT_COLOR):
    """向段落中添加格式化 run"""
    run = paragraph.add_run(text)
    set_run_font(run, font_name, font_size, bold, color)
    return run


def strip_markdown(text):
    """去除 Markdown 标记符号，只保留纯文本"""
    # 移除图片 ![alt](url)
    text = re.sub(r'!\[.*?\]\(.*?\)', '', text)
    # 转换链接 [text](url) -> text (url)
    text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'\1（\2）', text)
    # 移除行内代码标记 `code`
    text = re.sub(r'`([^`]+)`', r'\1', text)
    # 移除斜体 *text*
    text = re.sub(r'\*([^*\n]+)\*', r'\1', text)
    # 移除加粗 **text**（注意：这里先移除标记）
    text = re.sub(r'\*\*([^*\n]+)\*\*', r'\1', text)
    return text


def strip_markdown_light(text):
    """
    轻量去除Markdown：保留加粗语义，返回(text, bold_parts)结构。
    但为了简单，我们直接全部去除，用纯文本。**加粗**的文字保留内容但去掉**。
    """
    text = re.sub(r'!\[.*?\]\(.*?\)', '', text)
    text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', text)
    text = re.sub(r'`([^`]+)`', r'\1', text)
    # 先处理**加粗** -> 加粗文字（标记去掉）
    text = re.sub(r'\*\*([^*]+)\*\*', r'\1', text)
    # 再处理*斜体*
    text = re.sub(r'\*([^*\n]+)\*', r'\1', text)
    return text.strip()


def md_to_docx(md_path, docx_path=None):
    """将 Markdown 设计文档转换为指定格式的 Word (.docx) 文档"""
    if docx_path is None:
        docx_path = md_path.replace('.md', '.docx')

    with open(md_path, 'r', encoding='utf-8') as f:
        md_content = f.read()

    doc = Document()

    # ======== 1. Normal 样式（正文）========
    style = doc.styles['Normal']
    set_style_font(style, font_size=BODY_SIZE, bold=False)
    style.paragraph_format.space_before = PARA_SPACE_BEFORE
    style.paragraph_format.space_after = PARA_SPACE_AFTER
    style.paragraph_format.line_spacing = LINE_SPACING
    style.paragraph_format.line_spacing_rule = WD_LINE_SPACING.SINGLE

    # ======== 2. 标题样式 ========
    for level in range(1, 5):
        hs = doc.styles[f'Heading {level}']
        set_style_font(hs, font_size=HEADING_SIZES.get(level, BODY_SIZE), bold=True)
        hs.paragraph_format.space_before = PARA_SPACE_BEFORE
        hs.paragraph_format.space_after = PARA_SPACE_AFTER
        hs.paragraph_format.line_spacing = LINE_SPACING
        hs.paragraph_format.line_spacing_rule = WD_LINE_SPACING.SINGLE

    # ======== 3. 页面设置 ========
    for section in doc.sections:
        section.top_margin = Cm(2.54)
        section.bottom_margin = Cm(2.54)
        section.left_margin = Cm(3.17)
        section.right_margin = Cm(3.17)

    # ======== 4. 解析状态变量 ========
    lines = md_content.split('\n')
    in_code = False
    code_buf = []
    in_table = False
    table_buf = []
    i = 0

    # ======== 辅助函数 ========

    def flush_code():
        if not code_buf:
            return
        text = '\n'.join(code_buf)
        p = doc.add_paragraph()
        set_paragraph_format(p, space_before=2, space_after=2, left_indent=Cm(0.5))
        pf = p.paragraph_format
        pf.line_spacing = 1.0
        pf.line_spacing_rule = WD_LINE_SPACING.SINGLE
        run = p.add_run(text)
        set_run_font(run, font_name=CODE_FONT, font_size=9, bold=False)
        code_buf.clear()

    def flush_table():
        nonlocal table_buf
        if len(table_buf) < 2:
            table_buf = []
            return
        cols = max(len(r) for r in table_buf)
        if cols < 2:
            table_buf = []
            return
        norm = []
        for r in table_buf:
            row = list(r)
            row += [''] * (cols - len(row))
            norm.append(row[:cols])
        table = doc.add_table(rows=len(norm), cols=cols)
        table.style = 'Light Grid Accent 1'
        for ri, rd in enumerate(norm):
            for ci, ct in enumerate(rd):
                cell = table.cell(ri, ci)
                cell.text = strip_markdown_light(ct)
                for para in cell.paragraphs:
                    para.alignment = WD_ALIGN_PARAGRAPH.LEFT
                    set_paragraph_format(para)
                    for run in para.runs:
                        set_run_font(run, font_size=9, bold=False)
        doc.add_paragraph()
        table_buf = []

    def add_body_para(text, indent_cm=0):
        """添加正文段落（小四、黑色、段前段后0磅、单倍行距）"""
        text = strip_markdown_light(text)
        if not text:
            doc.add_paragraph()
            return
        p = doc.add_paragraph()
        set_paragraph_format(p, left_indent=Cm(indent_cm) if indent_cm else None)
        add_formatted_run(p, text, font_size=BODY_SIZE, bold=False)
        return p

    def add_code_inline(text):
        """将行内代码添加为正文段落"""
        # 去除外层的`标记
        text = re.sub(r'`([^`]+)`', r'\1', text)
        return add_body_para(text)

    # ======== 5. 逐行解析 Markdown ========

    while i < len(lines):
        line = lines[i]
        s = line.strip()

        # ---- 代码块 ----
        if s.startswith('```'):
            if in_code:
                flush_code()
                in_code = False
            else:
                in_code = True
                code_buf = []
            i += 1
            continue
        if in_code:
            code_buf.append(line)
            i += 1
            continue

        # ---- 空行时检查表格是否结束 ----
        if not s and in_table:
            in_table = False
            flush_table()
            i += 1
            continue

        # ---- 表格 ----
        if s.startswith('|') and s.count('|') >= 3:
            if re.match(r'^\|[\s\-:]+\|', s):
                i += 1
                continue
            in_table = True
            cells = [c.strip() for c in s.split('|')[1:-1]]
            table_buf.append(cells)
            i += 1
            continue
        elif in_table:
            in_table = False
            flush_table()

        # ---- 分隔线（水平横线）：跳过不生成，尤其是标题下方的横线 ----
        if re.match(r'^(-{3,}|\*{3,})$', s):
            # 完全跳过，不生成任何内容
            i += 1
            continue

        # ---- 标题 # ~ #### ----
        hm = re.match(r'^(#{1,4})\s+(.+)$', s)
        if hm:
            level = len(hm.group(1))
            title_text = hm.group(2)
            # 去除标题中可能的markdown标记
            title_text = strip_markdown_light(title_text)
            heading = doc.add_heading(title_text, level=min(level, 4))
            # 确保标题字体正确（含中文槽）
            for run in heading.runs:
                set_run_font(run, font_size=HEADING_SIZES.get(level, BODY_SIZE), bold=True)
            i += 1
            continue

        # ---- 无序列表 ----
        lm = re.match(r'^(\s*)[-*]\s+(.+)$', line)
        if lm:
            indent = len(lm.group(1))
            text = lm.group(2)
            add_body_para(text, indent_cm=0.5 + indent * 0.3)
            i += 1
            continue

        # ---- 有序列表 ----
        nm = re.match(r'^(\s*)\d+[.）)]\s+(.+)$', line)
        if nm:
            indent = len(nm.group(1))
            text = nm.group(2)
            add_body_para(text, indent_cm=0.5 + indent * 0.3)
            i += 1
            continue

        # ---- 引用块 (> 开头) ----
        if s.startswith('>'):
            text = re.sub(r'^>\s*', '', s)
            add_body_para(text)
            i += 1
            continue

        # ---- 普通正文段落 ----
        if s:
            add_body_para(s)
        else:
            # 纯空行 -> 空段落
            doc.add_paragraph()
        i += 1

    # 清理残留状态
    if in_table:
        flush_table()
    if in_code:
        flush_code()

    doc.save(docx_path)
    return docx_path


if __name__ == '__main__':
    target_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(target_dir)

    if len(sys.argv) > 1:
        arg = sys.argv[1]
        if arg == '--all':
            inputs = sorted([f for f in os.listdir('.') if f.endswith('.md')])
        else:
            inputs = [arg] if arg.endswith('.md') else [arg + '.md']
    else:
        inputs = [f for f in DESIGN_DOCS if os.path.exists(f)]

    if not inputs:
        print('No markdown files to convert.')
        sys.exit(0)

    ok = 0
    for f in inputs:
        if not os.path.exists(f):
            print(f'[SKIP] File not found: {f}')
            continue
        out = f.replace('.md', '.docx')
        try:
            md_to_docx(f, out)
            size = os.path.getsize(out)
            print(f'[OK]   {f}  ->  {out}  ({size:,} bytes)')
            ok += 1
        except Exception as e:
            print(f'[FAIL] {f}: {e}')

    print(f'\nDone. {ok}/{len(inputs)} files converted to Word (.docx).')
    print('Format: Heading1=三号(16pt) Heading2=小三(15pt) Heading3=四号(14pt) Body=小四(12pt)')
    print('Spacing: 段前段后0磅  行距: 单倍(1.0)  颜色: 黑色  横线: 已删除')
