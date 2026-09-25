import { http, HttpResponse } from 'msw'
import { products, categories, wrap, paginate } from '../data'

export const productHandlers = [
  // ---- 分类树 ----
  http.get('/api/product/category/tree', () => {
    return HttpResponse.json(wrap(categories))
  }),

  // ---- 一级分类列表 ----
  http.get('/api/product/category/top', () => {
    function flatten(list, parent = 0) {
      return list.flatMap(c => {
        const item = { ...c, parentId: parent }
        const children = c.children || []
        delete item.children
        return [item, ...flatten(children, c.id)]
      })
    }
    const flat = flatten(categories).filter(c => c.level === 1)
    return HttpResponse.json(wrap(flat))
  }),

  // ---- 商品列表（分页+搜索+筛选+排序） ----
  http.get('/api/product/page', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page')) || 1
    const size = Number(url.searchParams.get('size')) || 10
    const categoryId = Number(url.searchParams.get('categoryId'))
    const keyword = url.searchParams.get('keyword') || ''
    const sortBy = url.searchParams.get('sortBy') || 'default'
    const minPrice = Number(url.searchParams.get('minPrice')) || 0
    const maxPrice = Number(url.searchParams.get('maxPrice')) || Infinity

    let list = [...products]

    // 筛选分类（含子分类）
    if (categoryId) {
      function collectIds(tree, id) {
        const ids = [id]
        for (const c of tree) {
          if (c.id === id && c.children) {
            c.children.forEach(ch => ids.push(ch.id))
          }
          if (c.children) ids.push(...collectIds(c.children, id))
        }
        return ids
      }
      const ids = collectIds(categories, categoryId)
      list = list.filter(p => ids.includes(p.categoryId))
    }

    // 关键词搜索
    if (keyword) {
      const q = keyword.toLowerCase()
      list = list.filter(p =>
        p.name.toLowerCase().includes(q) ||
        (p.brand || '').toLowerCase().includes(q) ||
        (p.category || '').toLowerCase().includes(q)
      )
    }

    // 价格
    list = list.filter(p => p.price >= minPrice && p.price <= maxPrice)

    // 排序
    if (sortBy && sortBy !== 'default') {
      const asc = url.searchParams.get('asc') === 'true'
      const dir = asc ? 1 : -1
      switch (sortBy) {
        case 'price': list.sort((a, b) => (a.price - b.price) * dir); break
        case 'sales': list.sort((a, b) => (a.sales - b.sales) * dir); break
        case 'createTime':
        case 'newest': list.sort((a, b) => (a.id - b.id) * dir); break
      }
    }

    return HttpResponse.json(wrap(paginate(list, page, size)))
  }),

  // ---- 商品详情 ----
  http.get('/api/product/:id', ({ params }) => {
    const p = products.find(pp => pp.id === Number(params.id))
    if (!p) {
      return HttpResponse.json({ code: 404, message: '商品不存在', data: null, timestamp: Date.now() }, { status: 404 })
    }
    const cat = categories.find(c => c.id === p.categoryId)
    // 模拟返回带 attributes 的丰富详情
                    const attrMap = {
      1: [ // 男装
        { attrName: '材质', attrValue: '高品质面料' },
        { attrName: '版型', attrValue: '标准版型' },
        { attrName: '风格', attrValue: '简约时尚' },
      ],
      2: [ // 女装
        { attrName: '材质', attrValue: '优质面料' },
        { attrName: '版型', attrValue: '修身版型' },
        { attrName: '风格', attrValue: '优雅时尚' },
      ],
      3: [ // 数码
        { attrName: '材质', attrValue: '高品质材料' },
        { attrName: '重量', attrValue: '约200g' },
        { attrName: '保修', attrValue: '1年质保' },
      ],
      4: [ // 家居
        { attrName: '材质', attrValue: '环保材料' },
        { attrName: '尺寸', attrValue: '标准尺寸' },
        { attrName: '颜色', attrValue: '多色可选' },
      ],
      5: [ // 食品
        { attrName: '保质期', attrValue: '12个月' },
        { attrName: '产地', attrValue: '中国' },
        { attrName: '包装', attrValue: '精美包装' },
      ],
    },
        { attrName: '版型', attrValue: '标准版型' },
        { attrName: '风格', attrValue: '简约时尚' },
      ],
      2: [ // 女装
        { attrName: '材质', attrValue: '优质面料' },
        { attrName: '版型', attrValue: '修身版型' },
        { attrName: '风格', attrValue: '优雅时尚' },
      ],
      3: [ // 数码
        { attrName: '材质', attrValue: '高品质材料' },
        { attrName: '重量', attrValue: '约200g' },
        { attrName: '保修', attrValue: '1年质保' },
      ],
      4: [ // 家居
        { attrName: '材质', attrValue: '环保材料' },
        { attrName: '尺寸', attrValue: '标准尺寸' },
        { attrName: '颜色', attrValue: '多色可选' },
      ],
      5: [ // 食品
        { attrName: '保质期', attrValue: '12个月' },
        { attrName: '产地', attrValue: '中国' },
        { attrName: '包装', attrValue: '精美包装' },
      ],
    },
        { attrName: '版型', attrValue: '标准版型' },
        { attrName: '风格', attrValue: '简约时尚' },
      ],
      2: [ // 女装
        { attrName: '材质', attrValue: '优质面料' },
        { attrName: '版型', attrValue: '修身版型' },
        { attrName: '风格', attrValue: '优雅时尚' },
      ],
      3: [ // 数码
        { attrName: '材质', attrValue: '高品质材料' },
        { attrName: '重量', attrValue: '约200g' },
        { attrName: '保修', attrValue: '1年质保' },
      ],
      4: [ // 家居
        { attrName: '材质', attrValue: '环保材料' },
        { attrName: '尺寸', attrValue: '标准尺寸' },
        { attrName: '颜色', attrValue: '多色可选' },
      ],
      5: [ // 食品
        { attrName: '保质期', attrValue: '12个月' },
        { attrName: '产地', attrValue: '中国' },
        { attrName: '包装', attrValue: '精美包装' },
      ],
    },
        { attrName: '版型', attrValue: '标准版型' },
        { attrName: '风格', attrValue: '简约时尚' },
      ],
      2: [ // 女装
        { attrName: '材质', attrValue: '优质面料' },
        { attrName: '版型', attrValue: '修身版型' },
        { attrName: '风格', attrValue: '优雅时尚' },
      ],
      3: [ // 数码
        { attrName: '材质', attrValue: '高品质材料' },
        { attrName: '重量', attrValue: '约200g' },
        { attrName: '保修', attrValue: '1年质保' },
      ],
      4: [ // 家居
        { attrName: '材质', attrValue: '环保材料' },
        { attrName: '尺寸', attrValue: '标准尺寸' },
        { attrName: '颜色', attrValue: '多色可选' },
      ],
      5: [ // 食品
        { attrName: '保质期', attrValue: '12个月' },
        { attrName: '产地', attrValue: '中国' },
        { attrName: '包装', attrValue: '精美包装' },
      ],
    },
        { attrName: '重量', attrValue: '约200g' },
        { attrName: '保修', attrValue: '1年质保' },
      ],
      2: [ // 家居
        { attrName: '材质', attrValue: '环保材料' },
        { attrName: '尺寸', attrValue: '标准尺寸' },
        { attrName: '颜色', attrValue: '多色可选' },
        { attrName: '重量', attrValue: '约500g' },
      ],
      3: [ // 配饰
        { attrName: '材质', attrValue: '优质选材' },
        { attrName: '工艺', attrValue: '精细做工' },
        { attrName: '风格', attrValue: '简约时尚' },
      ],
    }
    const categoryAttrs = attrMap[p.categoryId] || attrMap[1]

    return HttpResponse.json(wrap({
      ...p,
      categoryName: cat?.name || p.category,
      subImages: [p.image, p.image, p.image],
      attributes: categoryAttrs,
      skuList: p.skuList || [],
    }))
  }),
]
