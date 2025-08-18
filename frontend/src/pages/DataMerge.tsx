import React, { useState, useEffect } from 'react';
import { 
  Download, 
  Search, 
  ChevronLeft, 
  ChevronRight,
  BarChart3,
  FileText,
  Database,
  CheckCircle,
  AlertCircle,
  XCircle,
  AlertTriangle
} from 'lucide-react';
import { api } from '../utils/api';

interface MergedOrderData {
  orderId: string;
  sku: string | null;
  quantity: number | null;
  sellingPrice: number | null;
  orderDateTime: string | null;
  productName: string | null;
  customerState: string | null;
  size: string | null;
  supplierListedPrice: number | null;
  supplierDiscountedPrice: number | null;
  packetId: string | null;
  reasonForCreditEntry: string | null;
  
  // Payment fields
  paymentId: string | null;
  amount: number | null;
  paymentDateTime: string | null;
  orderStatus: string | null;
  transactionId: string | null;
  finalSettlementAmount: number | null;
  priceType: string | null;
  totalSaleAmount: number | null;
  totalSaleReturnAmount: number | null;
  fixedFee: number | null;
  warehousingFee: number | null;
  returnPremium: number | null;
  meeshoCommissionPercentage: number | null;
  meeshoCommission: number | null;
  meeshoGoldPlatformFee: number | null;
  meeshoMallPlatformFee: number | null;
  returnShippingCharge: number | null;
  gstCompensation: number | null;
  shippingCharge: number | null;
  otherSupportServiceCharges: number | null;
  waivers: number | null;
  netOtherSupportServiceCharges: number | null;
  gstOnNetOtherSupportServiceCharges: number | null;
  tcs: number | null;
  tdsRatePercentage: number | null;
  tds: number | null;
  compensation: number | null;
  claims: number | null;
  recovery: number | null;
  dispatchDate: string | null;
  productGstPercentage: number | null;
  listingPriceInclTaxes: number | null;
  
  // Computed fields
  finalStatus: string | null;
  statusSource: string | null;
}

interface MergeStatistics {
  totalMergedRecords: number;
  statusSourceBreakdown: Record<string, number>;
  finalStatusBreakdown: Record<string, number>;
  uniqueOrders: number;
  dataQuality: {
    recordsWithSku: number;
    recordsWithoutSku: number;
    skuCoveragePercentage: number;
    recordsWithProductName: number;
    recordsWithQuantity: number;
  };
  warning?: string;
}

const DataMerge: React.FC = () => {
  const [mergedData, setMergedData] = useState<MergedOrderData[]>([]);
  const [statistics, setStatistics] = useState<MergeStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(50);
  const [totalRecords, setTotalRecords] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  
  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [sourceFilter, setSourceFilter] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>('');
  
  // Available filter options
  const [availableStatuses, setAvailableStatuses] = useState<string[]>([]);
  const [availableSources, setAvailableSources] = useState<string[]>([]);

  useEffect(() => {
    fetchData();
    fetchStatistics();
  }, [currentPage, pageSize, statusFilter, sourceFilter, searchTerm]);

  const fetchData = async () => {
    try {
      setLoading(true);
      
      let url = `/api/data-merge/merged-data/paginated?page=${currentPage}&size=${pageSize}`;
      
      // Add search query parameter if present
      if (searchTerm && searchTerm.trim()) {
        url += `&q=${encodeURIComponent(searchTerm.trim())}`;
      }
      
      if (statusFilter) {
        url = `/api/data-merge/merged-data/status/${encodeURIComponent(statusFilter)}`;
      } else if (sourceFilter) {
        url = `/api/data-merge/merged-data/source/${encodeURIComponent(sourceFilter)}`;
      }
      
      console.log('Fetching data from URL:', url);
      const response = await api.get(url);
      console.log('Response received:', response.data);
      
      if (response.data.data && response.data.totalRecords !== undefined) {
        setMergedData(response.data.data);
        setTotalRecords(response.data.totalRecords);
        setTotalPages(Math.ceil(response.data.totalRecords / response.data.pageSize));
      } else if (Array.isArray(response.data)) {
        setMergedData(response.data);
        setTotalRecords(response.data.length);
        setTotalPages(1);
      } else {
        console.warn('Unexpected response structure:', response.data);
        setMergedData([]);
        setTotalRecords(0);
        setTotalPages(0);
      }
    } catch (error) {
      console.error('Error fetching merged data:', error);
      setMergedData([]);
      setTotalRecords(0);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  };

  const fetchStatistics = async () => {
    try {
      const response = await api.get('/api/data-merge/statistics');
      setStatistics(response.data);
      
      if (response.data.finalStatusBreakdown) {
        setAvailableStatuses(Object.keys(response.data.finalStatusBreakdown));
      }
      if (response.data.statusSourceBreakdown) {
        setAvailableSources(Object.keys(response.data.statusSourceBreakdown));
      }
    } catch (error) {
      console.error('Error fetching statistics:', error);
      setStatistics(null);
    }
  };

  const clearFilters = () => {
    setStatusFilter('');
    setSourceFilter('');
    setSearchTerm('');
    setCurrentPage(0);
  };

  const exportToCSV = () => {
    if (mergedData.length === 0) return;
    
    const headers = [
      'Order ID', 'SKU', 'Product Name', 'Quantity', 'Selling Price', 'Customer State', 'Size',
      'Final Status', 'Status Source', 'Amount', 'Order Date', 'Payment Date', 'Dispatch Date',
      'Transaction ID', 'Total Sale Amount', 'Final Settlement Amount'
    ];
    
    const csvContent = [
      headers.join(','),
      ...mergedData.map(record => [
        record.orderId,
        record.sku || '',
        `"${record.productName || ''}"`,
        record.quantity || '',
        record.sellingPrice || '',
        record.customerState || '',
        record.size || '',
        record.finalStatus || '',
        record.statusSource || '',
        record.amount || '',
        record.orderDateTime || '',
        record.paymentDateTime || '',
        record.dispatchDate || '',
        record.transactionId || '',
        record.totalSaleAmount || '',
        record.finalSettlementAmount || ''
      ].join(','))
    ].join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `merged-data-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const getStatusIcon = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'delivered':
        return <CheckCircle className="h-4 w-4 text-green-600" />;
      case 'shipped':
        return <BarChart3 className="h-4 w-4 text-blue-600" />;
      case 'processing':
        return <AlertCircle className="h-4 w-4 text-yellow-600" />;
      case 'cancelled':
      case 'rto':
        return <XCircle className="h-4 w-4 text-red-600" />;
      default:
        return <FileText className="h-4 w-4 text-gray-600" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'delivered':
        return 'bg-green-100 text-green-800';
      case 'shipped':
        return 'bg-blue-100 text-blue-800';
      case 'processing':
        return 'bg-yellow-100 text-yellow-800';
      case 'cancelled':
      case 'rto':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  // Server-side search is now handled by the backend, so we use mergedData directly
  const displayData = mergedData;

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Data Merge - Orders & Payments</h1>
          <p className="text-gray-600">Merged dataset combining order and payment information with intelligent status resolution</p>
        </div>

        {/* Statistics Cards */}
        {statistics && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <div className="flex items-center">
                <div className="p-2 bg-blue-100 rounded-lg">
                  <Database className="h-6 w-6 text-blue-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Total Records</p>
                  <p className="text-2xl font-semibold text-gray-900">{Number(statistics?.totalMergedRecords || 0).toLocaleString()}</p>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <div className="flex items-center">
                <div className="p-2 bg-green-100 rounded-lg">
                  <CheckCircle className="h-6 w-6 text-green-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Unique Orders</p>
                  <p className="text-2xl font-semibold text-gray-900">{Number(statistics?.uniqueOrders || 0).toLocaleString()}</p>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <div className="flex items-center">
                <div className="p-2 bg-purple-100 rounded-lg">
                  <FileText className="h-6 w-6 text-purple-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Payment Source</p>
                  <p className="text-2xl font-semibold text-gray-900">
                    {Number((statistics?.statusSourceBreakdown || {})['PAYMENT_FILE'] || 0).toLocaleString()}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <div className="flex items-center">
                <div className="p-2 bg-orange-100 rounded-lg">
                  <AlertCircle className="h-6 w-6 text-orange-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Order Source</p>
                  <p className="text-2xl font-semibold text-gray-900">
                    {Number((statistics?.statusSourceBreakdown || {})['ORDER_FILE'] || 0).toLocaleString()}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Data Quality Warning */}
        {statistics?.warning && (
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-8">
            <div className="flex">
              <div className="flex-shrink-0">
                <AlertTriangle className="h-5 w-5 text-yellow-400" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-yellow-800">Data Quality Warning</h3>
                <div className="mt-2 text-sm text-yellow-700">
                  <p>{statistics.warning}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Data Quality Metrics */}
        {statistics?.dataQuality && (
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-8">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Data Quality Metrics</h3>
            <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
              <div className="text-center">
                <p className="text-2xl font-semibold text-green-600">{Number(statistics?.dataQuality?.skuCoveragePercentage || 0)}</p>
                <p className="text-sm text-gray-600">SKU Coverage</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-semibold text-blue-600">{Number(statistics?.dataQuality?.recordsWithSku || 0).toLocaleString()}</p>
                <p className="text-sm text-gray-600">Records with SKU</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-semibold text-red-600">{Number(statistics?.dataQuality?.recordsWithoutSku || 0).toLocaleString()}</p>
                <p className="text-sm text-gray-600">Records without SKU</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-semibold text-purple-600">{Number(statistics?.dataQuality?.recordsWithQuantity || 0).toLocaleString()}</p>
                <p className="text-sm text-gray-600">Records with Quantity</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-semibold text-orange-600">{Number(statistics?.dataQuality?.recordsWithProductName || 0).toLocaleString()}</p>
                <p className="text-sm text-gray-600">Records with Product Name</p>
              </div>
            </div>
          </div>
        )}

        {/* Filters and Controls */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-8">
          <div className="flex flex-col lg:flex-row gap-4 items-start lg:items-center justify-between">
            <div className="flex flex-col sm:flex-row gap-4 flex-1">
              {/* Search */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search orders, SKUs, products..."
                  value={searchTerm}
                  onChange={(e) => {
                    console.log('Search term changed to:', e.target.value);
                    setSearchTerm(e.target.value);
                  }}
                  className="pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Status Filter */}
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">All Statuses</option>
                {availableStatuses.map(status => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>

              {/* Source Filter */}
              <select
                value={sourceFilter}
                onChange={(e) => setSourceFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">All Sources</option>
                {availableSources.map(source => (
                  <option key={source} value={source}>{source}</option>
                ))}
              </select>
            </div>

            <div className="flex gap-2">
              <button
                onClick={clearFilters}
                className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                Clear Filters
              </button>
              <button
                onClick={exportToCSV}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 flex items-center"
              >
                <Download className="h-4 w-4 mr-2" />
                Export CSV
              </button>
            </div>
          </div>
        </div>

        {/* Data Table */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          {loading ? (
            <div className="flex items-center justify-center h-64">
              <div className="text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                <p className="text-gray-600">Loading merged data...</p>
              </div>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order ID</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">SKU</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Product Name</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Quantity</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Final Status</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Source</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order Date</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {displayData.map((record, index) => (
                      <tr key={`${record.orderId}-${index}`} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-medium text-gray-900">{record.orderId}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-900 font-mono">{record.sku || 'N/A'}</div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-sm text-gray-900 max-w-xs truncate" title={record.productName || 'N/A'}>
                            {record.productName || 'N/A'}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-900">{record.quantity ?? 'N/A'}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            {getStatusIcon(record.finalStatus || '')}
                            <span className={`ml-2 inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(record.finalStatus || '')}`}>
                              {record.finalStatus || 'N/A'}
                            </span>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                            record.statusSource === 'PAYMENT_FILE' ? 'bg-green-100 text-green-800' :
                            record.statusSource === 'ORDER_FILE' ? 'bg-blue-100 text-blue-800' :
                            'bg-gray-100 text-gray-800'
                          }`}>
                            {record.statusSource || 'N/A'}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-900">
                            {record.amount != null ? `₹${Number(record.amount).toLocaleString()}` : 'N/A'}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-500">
                            {record.orderDateTime ? new Date(record.orderDateTime).toLocaleDateString() : 'N/A'}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {!statusFilter && !sourceFilter && (
                <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
                  <div className="flex-1 flex justify-between sm:hidden">
                    <button
                      onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                      disabled={currentPage === 0}
                      className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                    >
                      Previous
                    </button>
                    <button
                      onClick={() => setCurrentPage(currentPage + 1)}
                      disabled={currentPage >= totalPages - 1}
                      className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                    >
                      Next
                    </button>
                  </div>
                  <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                    <div>
                      <p className="text-sm text-gray-700">
                        Showing <span className="font-medium">{currentPage * pageSize + 1}</span> to{' '}
                        <span className="font-medium">
                          {Math.min((currentPage + 1) * pageSize, totalRecords)}
                        </span>{' '}
                        of <span className="font-medium">{totalRecords}</span> results
                      </p>
                    </div>
                    <div>
                      <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                        <button
                          onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                          disabled={currentPage === 0}
                          className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                        >
                          <ChevronLeft className="h-5 w-5" />
                        </button>
                        <button
                          onClick={() => setCurrentPage(currentPage + 1)}
                          disabled={currentPage >= totalPages - 1}
                          className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                        >
                          <ChevronRight className="h-5 w-5" />
                        </button>
                      </nav>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Page Size Selector */}
        {!statusFilter && !sourceFilter && (
          <div className="mt-4 flex items-center justify-end">
            <label className="text-sm text-gray-700 mr-2">Page size:</label>
            <select
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                setCurrentPage(0);
              }}
              className="px-3 py-1 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value={25}>25</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
              <option value={200}>200</option>
            </select>
          </div>
        )}
      </div>
    </div>
  );
};

export default DataMerge;
