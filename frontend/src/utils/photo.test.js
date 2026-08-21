import { describe, expect, it } from 'vitest'
import { calculatePhotoDimensions } from './photo'

describe('photo utilities', () => {
  it('keeps images that are already small enough', () => {
    expect(calculatePhotoDimensions(1200, 900)).toEqual({ width: 1200, height: 900 })
  })

  it('scales landscape and portrait photos proportionally', () => {
    expect(calculatePhotoDimensions(4000, 3000)).toEqual({ width: 1600, height: 1200 })
    expect(calculatePhotoDimensions(3000, 4000)).toEqual({ width: 1200, height: 1600 })
  })
})
