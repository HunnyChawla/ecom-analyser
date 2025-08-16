import { useState } from 'react'
import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8080' })

export default function UploadData() {
  return (
    <div className="grid md:grid-cols-2 gap-6">
      <Uploader title="Upload Orders" endpoint="/api/upload/orders" />
      <Uploader title="Upload Payments" endpoint="/api/upload/payments" />
      <div className="bg-white rounded shadow p-4">
        <div className="flex items-center justify-between">
          <div>
            <div className="font-semibold mb-2">Upload SKU Prices</div>
            <UploaderInline endpoint="/api/upload/sku-prices" />
          </div>
          <a
            href="http://localhost:8080/api/sku-prices/template"
            className="px-3 py-1 rounded bg-gray-100 border text-sm"
          >
            Download Template
          </a>
        </div>
      </div>
    </div>
  )
}

function Uploader({ title, endpoint }: { title: string, endpoint: string }) {
  const [file, setFile] = useState<File | null>(null)
  const [status, setStatus] = useState<string>('')

  const onUpload = async () => {
    if (!file) return
    const form = new FormData()
    form.append('file', file)
    try {
      const res = await api.post(endpoint, form, { headers: { 'Content-Type': 'multipart/form-data' }})
      setStatus(String(res.data))
    } catch (e: any) {
      setStatus(e?.response?.data || 'Failed')
    }
  }

  return (
    <div className="bg-white rounded shadow p-4">
      <div className="font-semibold mb-2">{title}</div>
      <input type="file" accept=".xlsx,.xls,.csv,text/csv" onChange={e => setFile(e.target.files?.[0] || null)} />
      <div className="mt-2">
        <button onClick={onUpload} className="px-3 py-1 rounded bg-blue-600 text-white">Upload</button>
      </div>
      {status && <div className="mt-2 text-sm text-gray-600">{status}</div>}
    </div>
  )
}

function UploaderInline({ endpoint }: { endpoint: string }) {
  const [file, setFile] = useState<File | null>(null)
  const [status, setStatus] = useState<string>('')
  const onUpload = async () => {
    if (!file) return
    const form = new FormData()
    form.append('file', file)
    try {
      const res = await api.post(endpoint, form, { headers: { 'Content-Type': 'multipart/form-data' }})
      setStatus(String(res.data))
    } catch (e: any) {
      setStatus(e?.response?.data || 'Failed')
    }
  }
  return (
    <div>
      <input type="file" accept=".xlsx,.xls,.csv,text/csv" onChange={e => setFile(e.target.files?.[0] || null)} />
      <div className="mt-2">
        <button onClick={onUpload} className="px-3 py-1 rounded bg-blue-600 text-white text-sm">Upload</button>
      </div>
      {status && <div className="mt-2 text-xs text-gray-600">{status}</div>}
    </div>
  )
}


