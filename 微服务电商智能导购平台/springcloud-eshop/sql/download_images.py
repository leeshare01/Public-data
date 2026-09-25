import os
import re
import requests
import random
import time
from generate_data import PRODUCT_TEMPLATES

OUTPUT_DIR = "c:\\Users\\jie\\Desktop\\study\\frontend\\public\\images\\products"


def sanitize_filename(name):
    name = re.sub(r'[\\/:*?"<>|]', '', name)
    name = name.strip()
    if len(name) > 50:
        name = name[:50]
    return name


def generate_image_url(prod_id, image_index):
    seed = f"{prod_id}_{image_index}_{random.randint(1, 999999)}"
    return f"https://picsum.photos/seed/{seed}/600/600"


def download_image(url, save_path):
    try:
        response = requests.get(url, timeout=30)
        if response.status_code == 200:
            with open(save_path, 'wb') as f:
                f.write(response.content)
            return True, len(response.content)
        return False, 0
    except Exception as e:
        print(f"  Error: {e}")
        return False, 0


def main(max_images=568):
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    success_count = 0
    fail_count = 0
    sizes = []

    for idx, (name, subtitle, brand, desc, cat_id, orig, price, sales) in enumerate(PRODUCT_TEMPLATES):
        prod_id = idx + 1
        safe_name = sanitize_filename(name)
        
        print(f"Processing product {prod_id}/{len(PRODUCT_TEMPLATES)}: {name}")

        for i in range(1, 5):
            if success_count >= max_images:
                break
                
            save_path = os.path.join(OUTPUT_DIR, f"{safe_name}_{i}.jpg")

            if os.path.exists(save_path):
                continue

            url = generate_image_url(prod_id, i)
            success, size = download_image(url, save_path)

            if success:
                success_count += 1
                sizes.append(size)
                print(f"  Image {i} saved: {safe_name}_{i}.jpg ({size} bytes)")
            else:
                fail_count += 1

            time.sleep(0.2)

        if success_count >= max_images:
            break
            
        if (idx + 1) % 10 == 0:
            print(f"Processed {idx+1}/{len(PRODUCT_TEMPLATES)} products")

    print(f"\nDone!")
    print(f"Success: {success_count}")
    print(f"Failed: {fail_count}")

    if sizes:
        unique_sizes = len(set(sizes))
        print(f"Unique sizes: {unique_sizes} / {len(sizes)}")
        if unique_sizes > 1:
            print("✅ Images are different")
        else:
            print("❌ All images are the same!")


if __name__ == "__main__":
    import sys
    max_images = 568
    if len(sys.argv) > 1:
        try:
            max_images = int(sys.argv[1])
        except ValueError:
            pass
    main(max_images)