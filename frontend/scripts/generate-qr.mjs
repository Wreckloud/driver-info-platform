import QRCode from 'qrcode'
import { mkdir } from 'node:fs/promises'
import { resolve } from 'node:path'

const baseUrl = (process.env.PUBLIC_BASE_URL || '').replace(/\/$/, '')
if (!baseUrl || !/^https:\/\//i.test(baseUrl)) {
  throw new Error('请先设置 HTTPS 地址，例如 PUBLIC_BASE_URL=https://example.com')
}

const outputDir = resolve('dist-qr')
const targetUrl = `${baseUrl}/driver`
await mkdir(outputDir, { recursive: true })
await QRCode.toFile(resolve(outputDir, 'driver-qr.png'), targetUrl, { width: 1200, margin: 3, errorCorrectionLevel: 'H' })
await QRCode.toFile(resolve(outputDir, 'driver-qr.svg'), targetUrl, { type: 'svg', margin: 3, errorCorrectionLevel: 'H' })
console.log(`二维码已生成：${targetUrl}`)
