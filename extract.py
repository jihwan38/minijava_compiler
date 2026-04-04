import sys
import PyPDF2
reader = PyPDF2.PdfReader('3장 구문 분석.pdf')
text = '\n'.join(page.extract_text() for page in reader.pages)
with open('pdf_content.txt', 'w', encoding='utf-8') as f:
    f.write(text)
