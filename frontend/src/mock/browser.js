import { setupWorker } from 'msw/browser'
import { userHandlers } from './handlers/user'
import { productHandlers } from './handlers/product'
import { cartHandlers } from './handlers/cart'
import { orderHandlers } from './handlers/order'
import { aiHandlers } from './handlers/ai'

export const worker = setupWorker(
  ...userHandlers,
  ...productHandlers,
  ...cartHandlers,
  ...orderHandlers,
  ...aiHandlers,
)
