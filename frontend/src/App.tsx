import React, { useState, useEffect } from 'react'
import { fetchProducts } from './api/client'
import { Product, ProductStatus } from './types/Product'
import './App.css'

const App: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadProducts = async () => {
      try {
        const fetchedProducts = await fetchProducts()
        setProducts(fetchedProducts)
      } catch (err) {
        setError('Failed to load products')
        console.error('Error fetching products:', err)
      } finally {
        setLoading(false)
      }
    }

    loadProducts()
  }, [])

  const getStatusClass = (status: ProductStatus): string => {
    switch (status) {
      case ProductStatus.OUT_OF_STOCK:
        return 'out-of-stock'
      case ProductStatus.PRICE_REVIEW_PENDING:
        return 'review-pending'
      default:
        return 'active'
    }
  }

  if (loading) {
    return <div className="app">Loading...</div>
  }

  if (error) {
    return <div className="app">Error: {error}</div>
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>StockPulse Dashboard</h1>
        <p>Reactive Commerce Engine</p>
      </header>
      
      <main className="app-main">
        <div className="products-grid">
          {products.map((product) => (
            <div key={product.id} className={`product-card ${getStatusClass(product.status)}`}>
              <h2>{product.name}</h2>
              <p className="sku">{product.sku}</p>
              <p className="category">{product.category}</p>
              <div className="product-details">
                <span className="price">${product.currentPrice.toFixed(2)}</span>
                <span className={`stock ${product.stockLevel <= product.reorderThreshold ? 'low' : ''}`}>
                  Stock: {product.stockLevel}
                </span>
              </div>
              <div className="velocity">Velocity: {product.demandVelocity}</div>
              <div className="status">Status: {product.status}</div>
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}

export default App