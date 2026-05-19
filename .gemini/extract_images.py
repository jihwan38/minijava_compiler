import fitz
import os
import glob

out_dir = r".gemini\semantic_images"
os.makedirs(out_dir, exist_ok=True)

pdfs = glob.glob("*.pdf")

for pdf_path in pdfs:
    print(f"Processing {pdf_path}...")
    try:
        doc = fitz.open(pdf_path)
        count = 0
        base_name = os.path.splitext(os.path.basename(pdf_path))[0]
        clean_name = "".join([c if c.isalnum() else "_" for c in base_name])
        for i in range(len(doc)):
            page = doc[i]
            images = page.get_images(full=True)
            for img_index, img in enumerate(images):
                xref = img[0]
                base_image = doc.extract_image(xref)
                image_bytes = base_image["image"]
                image_ext = base_image["ext"]
                img_path = os.path.join(out_dir, f"{clean_name}_page_{i+1}_img_{img_index+1}.{image_ext}")
                with open(img_path, "wb") as f:
                    f.write(image_bytes)
                count += 1
        print(f"  -> Extracted {count} images from {pdf_path}")
    except Exception as e:
        print(f"  -> Error extracting images from {pdf_path}: {e}")
