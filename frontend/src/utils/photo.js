const MAX_DIMENSION = 1600
const MAX_SOURCE_SIZE = 20 * 1024 * 1024
const MAX_COMPRESSED_SIZE = 2 * 1024 * 1024

export function calculatePhotoDimensions(width, height, maxDimension = MAX_DIMENSION) {
  if (width <= maxDimension && height <= maxDimension) return { width, height }
  const scale = maxDimension / Math.max(width, height)
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale))
  }
}

export async function compressPhoto(file) {
  if (!file?.type?.startsWith('image/')) throw new Error('请选择图片文件')
  if (file.size > MAX_SOURCE_SIZE) throw new Error('单张原图不能超过 20MB')

  const image = await decodePhoto(file)
  try {
    if (!image.width || !image.height) throw new Error('无法读取这张照片，请重新选择')
    const dimensions = calculatePhotoDimensions(image.width, image.height)
    const canvas = document.createElement('canvas')
    canvas.width = dimensions.width
    canvas.height = dimensions.height
    const context = canvas.getContext('2d', { alpha: false })
    if (!context) throw new Error('当前浏览器无法压缩照片，请更换浏览器后重试')
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.drawImage(image.source, 0, 0, canvas.width, canvas.height)

    let blob = await canvasToBlob(canvas, 0.8)
    if (blob.size > 1.5 * 1024 * 1024) blob = await canvasToBlob(canvas, 0.68)
    if (blob.size > MAX_COMPRESSED_SIZE) blob = await canvasToBlob(canvas, 0.56)
    if (blob.size > MAX_COMPRESSED_SIZE) throw new Error('照片压缩后仍然过大，请重新拍摄')

    const baseName = (file.name || 'photo').replace(/\.[^.]+$/, '').slice(0, 60) || 'photo'
    return new File([blob], `${baseName}.jpg`, { type: 'image/jpeg', lastModified: Date.now() })
  } finally {
    image.close()
  }
}

async function decodePhoto(file) {
  if (typeof createImageBitmap === 'function') {
    try {
      const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })
      return { source: bitmap, width: bitmap.width, height: bitmap.height, close: () => bitmap.close() }
    } catch {
      // 某些微信内置浏览器不支持 createImageBitmap，继续使用 Image 解码。
    }
  }

  const url = URL.createObjectURL(file)
  try {
    const image = await new Promise((resolve, reject) => {
      const element = new Image()
      element.onload = () => resolve(element)
      element.onerror = () => reject(new Error('无法读取这张照片，请重新选择'))
      element.src = url
    })
    return { source: image, width: image.naturalWidth, height: image.naturalHeight, close: () => {} }
  } finally {
    URL.revokeObjectURL(url)
  }
}

function canvasToBlob(canvas, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(blob)
      else reject(new Error('照片压缩失败，请重新选择'))
    }, 'image/jpeg', quality)
  })
}
