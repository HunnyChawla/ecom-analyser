import { useState } from 'react'
import { api } from '../utils/api'

// Types for the upload response
interface UploadResponse {
  message?: string
  warningCount?: number
  warnings?: string[]
  records?: number
  success?: boolean
  error?: string
}

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
                            href="http://192.168.1.8:8080/api/sku-prices/template"
            className="px-3 py-1 rounded bg-gray-100 border text-sm hover:bg-gray-200 transition-colors"
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
  const [isLoading, setIsLoading] = useState(false)
  const [response, setResponse] = useState<UploadResponse | null>(null)
  const [error, setError] = useState<string>('')

  const onUpload = async () => {
    if (!file) return
    
    setIsLoading(true)
    setError('')
    setResponse(null)
    
    const form = new FormData()
    form.append('file', file)
    
    try {
      const res = await api.post(endpoint, form, { 
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      setResponse(res.data)
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.response?.data || 'Upload failed')
    } finally {
      setIsLoading(false)
    }
  }

  const resetUpload = () => {
    setFile(null)
    setResponse(null)
    setError('')
  }

  return (
    <div className="bg-white rounded shadow p-4">
      <div className="font-semibold mb-3 text-lg">{title}</div>
      
      {/* File Input */}
      <div className="mb-3">
        <input 
          type="file" 
          accept=".xlsx,.xls,.csv,text/csv" 
          onChange={e => setFile(e.target.files?.[0] || null)}
          className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
        />
      </div>
      
      {/* Upload Button */}
      <div className="mb-3">
        <button 
          onClick={onUpload} 
          disabled={!file || isLoading}
          className={`px-4 py-2 rounded font-medium transition-colors ${
            !file || isLoading 
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed' 
              : 'bg-blue-600 text-white hover:bg-blue-700'
          }`}
        >
          {isLoading ? (
            <span className="flex items-center">
              <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Uploading...
            </span>
          ) : (
            'Upload'
          )}
        </button>
        
        {response && (
          <button 
            onClick={resetUpload}
            className="ml-2 px-3 py-2 rounded bg-gray-500 text-white hover:bg-gray-600 transition-colors"
          >
            Upload Another
          </button>
        )}
      </div>
      
      {/* Loading State */}
      {isLoading && (
        <div className="mb-3 p-3 bg-blue-50 border border-blue-200 rounded">
          <div className="flex items-center text-blue-700">
            <svg className="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Processing file... Please wait
          </div>
        </div>
      )}
      
      {/* Success Response */}
      {response && !error && (
        <div className="mb-3 p-3 bg-green-50 border border-green-200 rounded">
          <div className="text-green-800">
            <div className="font-semibold mb-2">✅ Upload Successful!</div>
            {response.message && <div className="mb-2">{response.message}</div>}
            {response.records && <div className="text-sm">Records processed: {response.records}</div>}
            {response.warningCount && response.warningCount > 0 && (
              <div className="mt-2">
                <div className="text-amber-700 font-medium">
                  ⚠️ {response.warningCount} warnings found
                </div>
                <details className="mt-2">
                  <summary className="cursor-pointer text-sm text-amber-600 hover:text-amber-800">
                    Click to view warnings
                  </summary>
                  <div className="mt-2 max-h-40 overflow-y-auto">
                    {response.warnings?.map((warning, index) => (
                      <div key={index} className="text-xs text-amber-700 mb-1 p-2 bg-amber-50 rounded">
                        {warning}
                      </div>
                    ))}
                  </div>
                </details>
              </div>
            )}
          </div>
        </div>
      )}
      
      {/* Error Response */}
      {error && (
        <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded">
          <div className="text-red-800">
            <div className="font-semibold mb-2">❌ Upload Failed</div>
            <div className="text-sm">{error}</div>
          </div>
        </div>
      )}
    </div>
  )
}

function UploaderInline({ endpoint }: { endpoint: string }) {
  const [file, setFile] = useState<File | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [response, setResponse] = useState<UploadResponse | null>(null)
  const [error, setError] = useState<string>('')

  const onUpload = async () => {
    if (!file) return
    
    setIsLoading(true)
    setError('')
    setResponse(null)
    
    const form = new FormData()
    form.append('file', file)
    
    try {
      const res = await api.post(endpoint, form, { 
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      setResponse(res.data)
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.response?.data || 'Upload failed')
    } finally {
      setIsLoading(false)
    }
  }

  const resetUpload = () => {
    setFile(null)
    setResponse(null)
    setError('')
  }

  return (
    <div>
              <input 
          type="file" 
          accept=".xlsx,.xls,.csv,text/csv" 
          onChange={e => setFile(e.target.files?.[0] || null)}
          className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
        />
      
      <div className="mt-2">
        <button 
          onClick={onUpload} 
          disabled={!file || isLoading}
          className={`px-3 py-2 rounded text-sm font-medium transition-colors ${
            !file || isLoading 
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed' 
              : 'bg-blue-600 text-white hover:bg-blue-700'
          }`}
        >
          {isLoading ? (
            <span className="flex items-center">
              <svg className="animate-spin -ml-1 mr-2 h-3 w-3 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Uploading...
            </span>
          ) : (
            'Upload'
          )}
        </button>
        
        {response && (
          <button 
            onClick={resetUpload}
            className="ml-2 px-2 py-2 rounded bg-gray-500 text-white hover:bg-gray-600 transition-colors text-xs"
          >
            Reset
          </button>
        )}
      </div>
      
      {/* Loading State */}
      {isLoading && (
        <div className="mt-2 p-2 bg-blue-50 border border-blue-200 rounded">
          <div className="flex items-center text-blue-700 text-xs">
            <svg className="animate-spin -ml-1 mr-1 h-3 w-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Processing...
          </div>
        </div>
      )}
      
      {/* Success Response */}
      {response && !error && (
        <div className="mt-2 p-2 bg-green-50 border border-green-200 rounded">
          <div className="text-green-800 text-xs">
            <div className="font-medium mb-1">✅ Success!</div>
            {response.message && <div className="mb-1">{response.message}</div>}
            {response.warningCount && response.warningCount > 0 && (
              <div className="text-amber-700">
                ⚠️ {response.warningCount} warnings
              </div>
            )}
          </div>
        </div>
      )}
      
      {/* Error Response */}
      {error && (
        <div className="mt-2 p-2 bg-red-50 border border-red-200 rounded">
          <div className="text-red-800 text-xs">
            <div className="font-medium mb-1">❌ Failed</div>
            <div>{error}</div>
          </div>
        </div>
      )}
    </div>
  )
}



