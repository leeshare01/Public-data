import { http, HttpResponse, delay } from 'msw'
import { conversations, getUserFromToken, wrap, paginate } from '../data'

const aiReplies = [
  {
    keywords: ['耳机', '降噪'],
    reply: '为您推荐 **无线降噪耳机 Pro**，目前是我们店内的爆款产品！\n\n✅ 40dB 深度降噪，通勤/办公/学习都适用\n✅ Hi-Res 认证音质，30 小时超长续航\n✅ 蓝牙 5.3，支持多设备切换\n\n目前活动价 **¥1,299**（原价¥1,599），性价比非常高！',
    products: [{ productId: 1, name: '无线降噪耳机 Pro', price: 1299, image: 'https://picsum.photos/seed/headphones/400/400', reason: '旗舰降噪，学生党必备' }],
  },
  {
    keywords: ['手表', '智能', '运动'],
    reply: '根据您的需求，推荐 **智能手表 Series 7**，这款是我们的新品！\n\n⌚ 全天候视网膜屏，显示清晰细腻\n❤️ 血氧/心率/睡眠全面健康监测\n🏊 50 米防水，100+ 运动模式\n\n活动价 **¥2,499**（原价¥2,999），三色可选！',
    products: [{ productId: 2, name: '智能手表 Series 7', price: 2499, image: 'https://picsum.photos/seed/watch/400/400', reason: '全维度健康监测' }],
  },
  {
    keywords: ['键盘', '机械'],
    reply: '为您推荐 **机械键盘 RGB**，码字游戏两不误！\n\n⌨️ 全键热插拔轴座，可自由换轴\n🎨 1680 万色 RGB 背光，氛围拉满\n🔗 三模连接（有线/2.4G/蓝牙）\n\n仅 **¥699**（原价¥899），青轴/红轴/茶轴三种轴体可选！',
    products: [{ productId: 3, name: '机械键盘 RGB', price: 699, image: 'https://picsum.photos/seed/keyboard/400/400', reason: '全键热插拔，三模连接' }],
  },
  {
    keywords: ['音箱', '蓝牙', '音响'],
    reply: '为您推荐 **便携蓝牙音箱**，户外室内都能打！\n\n🔊 IPX7 防水，户外下雨也不怕\n🔋 20 小时续航，充一次用一天\n🎵 TWS 串联立体声，两个配对更震撼\n\n特惠价 **¥399**（原价¥499），三色可选！',
    products: [{ productId: 4, name: '便携蓝牙音箱', price: 399, image: 'https://picsum.photos/seed/speaker/400/400', reason: 'IPX7防水，超长续航' }],
  },
  {
    keywords: ['台灯', '护眼', '灯'],
    reply: '来看这款 **极简台灯**，北欧设计护眼首选！\n\n💡 Ra>95 高显色，色彩真实不刺眼\n🔄 三档色温无极调光，阅读/办公/休息都适合\n🔘 触控滑动调光，操作优雅\n\n仅 **¥299**（原价¥399），双色可选。',
    products: [{ productId: 5, name: '极简台灯', price: 299, image: 'https://picsum.photos/seed/lamp/400/400', reason: 'Ra>95护眼无频闪' }],
  },
  {
    keywords: ['推荐', '什么好', '性价比', '学生', '平价'],
    reply: '为您精选几款高性价比好物：\n\n1️⃣ **无线降噪耳机 Pro** ¥1,299 — 旗舰降噪，学生党宿舍必备\n2️⃣ **便携蓝牙音箱** ¥399 — 户外学习/休闲放松好伴侣\n3️⃣ **手机支架** ¥79 — 铝合金材质，追剧上网课神器\n4️⃣ **极简台灯** ¥299 — Ra>95 护眼无频闪，学习必备\n\n您对哪一款感兴趣？我可以帮您查看详细信息和库存情况～',
    products: [{ productId: 1, name: '无线降噪耳机 Pro', price: 1299, image: 'https://picsum.photos/seed/headphones/400/400', reason: '旗舰降噪' }, { productId: 4, name: '便携蓝牙音箱', price: 399, image: 'https://picsum.photos/seed/speaker/400/400', reason: '户外休闲' }, { productId: 11, name: '手机支架', price: 79, image: 'https://picsum.photos/seed/stand/400/400', reason: '追剧神器' }, { productId: 5, name: '极简台灯', price: 299, image: 'https://picsum.photos/seed/lamp/400/400', reason: '护眼必备' }],
  },
  {
    keywords: [],
    reply: '你好！欢迎来到 **商城** 🎉\n\n我是 AI 智能导购，可以帮你：\n\n🔍 **找商品** — 告诉我你的需求，我帮你筛选\n💬 **回答问题** — 商品参数、功能对比都可以问我\n🎯 **推荐好物** — 告诉我预算和喜好，精准推荐\n\n例如：\n• "推荐适合学生的平价耳机"\n• "想买一款智能手表"\n• "有没有性价比高的蓝牙音箱"\n\n请直接说出你的需求吧！',
    products: [],
  },
]

function matchReply(text) {
  const q = text.toLowerCase()
  for (const r of aiReplies) {
    for (const kw of r.keywords) {
      if (q.includes(kw)) return r
    }
  }
  return aiReplies[aiReplies.length - 1]
}

export const aiHandlers = [
  // ---- 健康检测 ----
  http.get('/api/ai/health', () => {
    return HttpResponse.json(wrap({
      status: 'UP',
      deepseekStatus: 'UP',
      milvusStatus: 'UP',
      lastExceptionTime: null,
    }))
  }),

  // ---- 非流式聊天 ----
  http.post('/api/ai/chat', async ({ request }) => {
    const user = checkAuth(request)
    const body = await request.json()
    const { message } = body

    await delay(500 + Math.random() * 1000)

    const match = matchReply(message)
    return HttpResponse.json(wrap({
      conversationId: 1,
      reply: match.reply,
      productIds: match.products.map(p => p.productId),
      recommendations: match.products,
      knowledgeRefs: [{ docId: 5, title: '商品导购FAQ' }],
    }))
  }),

  // ---- 流式聊天 SSE ----
  http.post('/api/ai/chat/stream', async ({ request }) => {
    const body = await request.json()
    const { message } = body

    await delay(200)
    const match = matchReply(message)
    const encoder = new TextEncoder()
    const stream = new ReadableStream({
      async start(controller) {
        // reasoning
        controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: 'reasoning', content: '正在分析您的需求...' })}\n\n`))
        await delay(300 + Math.random() * 500)

        // text chunks
        const fullText = match.reply
        const chunkSize = 5
        for (let i = 0; i < fullText.length; i += chunkSize) {
          const chunk = fullText.slice(i, i + chunkSize)
          controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: 'text', content: chunk })}\n\n`))
          await delay(30 + Math.random() * 60)
        }

        // product cards
        for (const p of match.products) {
          controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: 'product', data: p })}\n\n`))
          await delay(100)
        }

        // done
        controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: 'done', conversationId: 1, recommendations: match.products })}\n\n`))
        controller.close()
      },
    })

    return new HttpResponse(stream, {
      headers: {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
      },
    })
  }),

  // ---- 对话列表 ----
  http.get('/api/ai/conversations', ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page')) || 1
    const size = Number(url.searchParams.get('size')) || 20
    return HttpResponse.json(wrap(paginate(conversations, page, size)))
  }),

  // ---- 获取对话消息 ----
  http.get('/api/ai/conversations/:id/messages', ({ params }) => {
    return HttpResponse.json(wrap(paginate([
      { role: 'user', content: '推荐适合学生的平价耳机', contentType: 'text', createTime: new Date().toLocaleString() },
      { role: 'assistant', content: '为您推荐无线降噪耳机 Pro...', contentType: 'text', relatedProductIds: [1], createTime: new Date().toLocaleString() },
    ])))
  }),

  // ---- 删除对话 ----
  http.delete('/api/ai/conversations/:id', () => {
    return HttpResponse.json(wrap(null))
  }),
]

function checkAuth(request) {
  const auth = request.headers.get('Authorization') || ''
  return getUserFromToken(auth.replace('Bearer ', ''))
}
