"""
从后端商品模板数据生成前端 Mock 数据
"""
from generate_data import PRODUCT_TEMPLATES
import random

output = """function img(id) {
  return `/images/products/product_${id}_1.jpg`
}

export const mockProducts = [
"""

for idx, (name, subtitle, brand, desc, cat_id, orig, price, sales) in enumerate(PRODUCT_TEMPLATES):
    prod_id = idx + 1
    category_map = {
        11: "男装", 12: "男装", 13: "男装", 14: "男装",
        15: "女装", 16: "女装", 17: "女装", 18: "女装",
        21: "数码", 22: "数码", 23: "数码", 24: "数码", 25: "数码",
        31: "家居", 32: "家居", 33: "家居", 34: "家居", 35: "家居",
        41: "食品", 42: "食品",
        61: "运动", 62: "运动", 63: "运动",
    }
    category = category_map.get(cat_id, "其他")
    
    tag = "热销" if sales > 3000 else ("新品" if prod_id <= 10 else None)
    
    desc_short = desc[:100] + "..." if len(desc) > 100 else desc
    
    output += f"""  {{
    id: {prod_id}, name: "{name}", price: {price}, originalPrice: {orig},
    image: img({prod_id}),
    category: "{category}", tag: {repr(tag)},
    description: "{desc_short.replace('"', '\\"')}",
    specs: [],
    rating: {round(4.0 + random.random() * 0.9, 1)}, reviewsCount: {sales}, sales: {sales},
    colors: null, sizes: null,
  }},
"""

output += "]"

with open("c:\\Users\\jie\\Desktop\\study\\frontend\\src\\mock\\products.js", "w", encoding="utf-8") as f:
    f.write(output)

print(f"Mock数据已生成，共 {len(PRODUCT_TEMPLATES)} 个商品")
