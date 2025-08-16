import { BrowserRouter, Routes, Route, Link, NavLink } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import UploadData from './pages/UploadData'
import SkuGroupManagement from './pages/SkuGroupManagement'
import DataMerge from './pages/DataMerge'

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50 text-gray-900">
        <header className="bg-white shadow">
          <div className="max-w-7xl mx-auto px-4 py-4 flex justify-between items-center">
            <Link to="/" className="text-xl font-semibold">EcomAnalyser</Link>
            <nav className="space-x-4">
              <NavLink to="/" end className={({isActive}) => isActive ? 'font-semibold' : ''}>Dashboard</NavLink>
              <NavLink to="/upload" className={({isActive}) => isActive ? 'font-semibold' : ''}>Upload</NavLink>
              <NavLink to="/sku-groups" className={({isActive}) => isActive ? 'font-semibold' : ''}>SKU Groups</NavLink>
              <NavLink to="/data-merge" className={({isActive}) => isActive ? 'font-semibold' : ''}>Data Merge</NavLink>
            </nav>
          </div>
        </header>
        <main className="max-w-7xl mx-auto p-4">
          <Routes>
            <Route path="/" element={<Dashboard/>} />
            <Route path="/upload" element={<UploadData/>} />
            <Route path="/sku-groups" element={<SkuGroupManagement/>} />
            <Route path="/data-merge" element={<DataMerge/>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
