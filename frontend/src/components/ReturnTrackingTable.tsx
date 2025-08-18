import React, { useState, useEffect } from 'react';
import axios from 'axios';

const api = axios.create({ baseURL: 'http://localhost:8080' });

interface ReturnOrder {
  id: number;
  orderId: string;
  skuId: string;
  quantity: number;
  returnAmount: number;
  orderStatus: string;
  orderDate: string;
  returnStatus: 'PENDING_RECEIPT' | 'RECEIVED' | 'NOT_RECEIVED';
  receivedDate?: string;
  receivedBy?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

interface ReturnSummary {
  totalOrders: number;
  pendingReceipts: number;
  receivedOrders: number;
  notReceivedOrders: number;
  statusBreakdown: Record<string, number>;
}

const ReturnTrackingTable: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'pending' | 'received' | 'not-received'>('pending');
  const [orders, setOrders] = useState<ReturnOrder[]>([]);
  const [summary, setSummary] = useState<ReturnSummary | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string>('');
  
  // Search and filter states
  const [searchOrderId, setSearchOrderId] = useState('');
  const [searchSkuId, setSearchSkuId] = useState('');
  const [searchStartDate, setSearchStartDate] = useState('');
  const [searchEndDate, setSearchEndDate] = useState('');
  
  // Modal states
  const [showMarkReceivedModal, setShowMarkReceivedModal] = useState(false);
  const [showMarkNotReceivedModal, setShowMarkNotReceivedModal] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<ReturnOrder | null>(null);
  const [receivedBy, setReceivedBy] = useState('');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch data based on active tab
  const fetchData = async () => {
    setIsLoading(true);
    setError('');
    
    try {
      let endpoint = '';
      switch (activeTab) {
        case 'pending':
          endpoint = '/api/return-tracking/status/PENDING_RECEIPT';
          break;
        case 'received':
          endpoint = '/api/return-tracking/status/RECEIVED';
          break;
        case 'not-received':
          endpoint = '/api/return-tracking/status/NOT_RECEIVED';
          break;
      }
      
      const response = await api.get(endpoint);
      if (response.data.success) {
        setOrders(response.data.orders || []);
      } else {
        setError(response.data.error || 'Failed to fetch orders');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to fetch orders');
      console.error('Error fetching orders:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Fetch summary data
  const fetchSummary = async () => {
    try {
      const response = await api.get('/api/return-tracking/summary');
      if (response.data.success) {
        setSummary(response.data.summary);
      }
    } catch (err: any) {
      console.error('Error fetching summary:', err);
    }
  };

  // Search orders
  const searchOrders = async () => {
    setIsLoading(true);
    setError('');
    
    try {
      // Validate date range if both dates are provided
      if (searchStartDate && searchEndDate) {
        const startDate = new Date(searchStartDate);
        const endDate = new Date(searchEndDate);
        if (startDate > endDate) {
          setError('Start date cannot be after end date');
          setIsLoading(false);
          return;
        }
      }
      
      const params: any = {};
      if (searchOrderId.trim()) params.orderId = searchOrderId.trim();
      if (searchSkuId.trim()) params.skuId = searchSkuId.trim();
      if (searchStartDate) params.start = searchStartDate;
      if (searchEndDate) params.end = searchEndDate;
      
      // Only search if at least one parameter is provided
      if (Object.keys(params).length === 0) {
        setError('Please provide at least one search criteria');
        setIsLoading(false);
        return;
      }
      
      const response = await api.get('/api/return-tracking/search', { params });
      if (response.data.success) {
        setOrders(response.data.orders || []);
        if (response.data.orders.length === 0) {
          setError('No orders found matching your search criteria');
        }
      } else {
        setError(response.data.error || 'Search failed');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Search failed');
      console.error('Error searching orders:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Sync return orders
  const syncReturnOrders = async () => {
    setIsLoading(true);
    setError('');
    
    try {
      const response = await api.post('/api/return-tracking/sync');
      if (response.data.success) {
        alert(`Sync completed! Added: ${response.data.addedCount}, Updated: ${response.data.updatedCount}`);
        fetchData();
        fetchSummary();
      } else {
        setError(response.data.error || 'Sync failed');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Sync failed');
      console.error('Error syncing orders:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Mark order as received
  const handleMarkAsReceived = async () => {
    if (!selectedOrder || !receivedBy.trim()) return;
    
    setIsSubmitting(true);
    try {
      const response = await api.post('/api/return-tracking/mark-received', null, {
        params: {
          orderId: selectedOrder.orderId,
          receivedBy: receivedBy.trim(),
          notes: notes.trim() || undefined
        }
      });
      
      if (response.data.success) {
        alert('Order marked as received successfully!');
        setShowMarkReceivedModal(false);
        setSelectedOrder(null);
        setReceivedBy('');
        setNotes('');
        fetchData();
        fetchSummary();
      } else {
        setError(response.data.error || 'Failed to mark order as received');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to mark order as received');
      console.error('Error marking order as received:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Mark order as not received
  const handleMarkAsNotReceived = async () => {
    if (!selectedOrder || !notes.trim()) return;
    
    setIsSubmitting(true);
    try {
      const response = await api.post('/api/return-tracking/mark-not-received', null, {
        params: {
          orderId: selectedOrder.orderId,
          notes: notes.trim()
        }
      });
      
      if (response.data.success) {
        alert('Order marked as not received successfully!');
        setShowMarkNotReceivedModal(false);
        setSelectedOrder(null);
        setNotes('');
        fetchData();
        fetchSummary();
      } else {
        setError(response.data.error || 'Failed to mark order as not received');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to mark order as not received');
      console.error('Error marking order as not received:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Open mark received modal
  const openMarkReceivedModal = (order: ReturnOrder) => {
    setSelectedOrder(order);
    setReceivedBy('');
    setNotes('');
    setShowMarkReceivedModal(true);
  };

  // Open mark not received modal
  const openMarkNotReceivedModal = (order: ReturnOrder) => {
    setSelectedOrder(order);
    setNotes('');
    setShowMarkNotReceivedModal(true);
  };

  // Format currency
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  };

  // Format date
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN');
  };

  // Get status badge class
  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'PENDING_RECEIPT':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'RECEIVED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'NOT_RECEIVED':
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  // Get status display text
  const getStatusDisplayText = (status: string) => {
    switch (status) {
      case 'PENDING_RECEIPT':
        return 'Pending Receipt';
      case 'RECEIVED':
        return 'Received';
      case 'NOT_RECEIVED':
        return 'Not Received';
      default:
        return status;
    }
  };

  useEffect(() => {
    fetchData();
    fetchSummary();
  }, [activeTab]);

  return (
    <div className="bg-white rounded-lg shadow-lg p-6">
      {/* Header and Controls */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center mb-6 gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Return Tracking</h2>
          <p className="text-gray-600 mt-1">
            Track return orders and manage their receipt status
          </p>
        </div>
        
        <div className="flex gap-3">
          <button
            onClick={syncReturnOrders}
            disabled={isLoading}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
          >
            {isLoading ? 'Syncing...' : 'Sync Returns'}
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="text-blue-600 text-sm font-medium">Total Returns</div>
            <div className="text-2xl font-bold text-blue-900">{summary.totalOrders}</div>
          </div>
          
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div className="text-yellow-600 text-sm font-medium">Pending Receipt</div>
            <div className="text-2xl font-bold text-yellow-900">{summary.pendingReceipts}</div>
          </div>
          
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="text-green-600 text-sm font-medium">Received</div>
            <div className="text-2xl font-bold text-green-900">{summary.receivedOrders}</div>
          </div>
          
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="text-red-600 text-sm font-medium">Not Received</div>
            <div className="text-2xl font-bold text-red-900">{summary.notReceivedOrders}</div>
          </div>
        </div>
      )}

      {/* Search Section */}
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Search & Filter</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          <input
            type="text"
            placeholder="Order ID"
            value={searchOrderId}
            onChange={(e) => setSearchOrderId(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          
          <input
            type="text"
            placeholder="SKU ID"
            value={searchSkuId}
            onChange={(e) => setSearchSkuId(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          
          <input
            type="date"
            value={searchStartDate}
            onChange={(e) => setSearchStartDate(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          
          <input
            type="date"
            value={searchEndDate}
            onChange={(e) => setSearchEndDate(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          
          <button
            onClick={searchOrders}
            disabled={isLoading}
            className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
          >
            {isLoading ? 'Searching...' : 'Search'}
          </button>
          
          <button
            onClick={() => {
              setSearchOrderId('');
              setSearchSkuId('');
              setSearchStartDate('');
              setSearchEndDate('');
              setError('');
              fetchData(); // Reset to current tab data
            }}
            className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
          >
            Clear
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('pending')}
            className={`py-2 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'pending'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Pending Receipt ({summary?.pendingReceipts || 0})
          </button>
          
          <button
            onClick={() => setActiveTab('received')}
            className={`py-2 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'received'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Received ({summary?.receivedOrders || 0})
          </button>
          
          <button
            onClick={() => setActiveTab('not-received')}
            className={`py-2 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'not-received'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Not Received ({summary?.notReceivedOrders || 0})
          </button>
        </nav>
      </div>

      {/* Error Display */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <div className="text-red-800">
            <strong>Error:</strong> {error}
          </div>
        </div>
      )}

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center items-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <span className="ml-2 text-gray-600">Loading orders...</span>
        </div>
      )}

      {/* Orders Table */}
      {!isLoading && !error && orders.length > 0 && (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Order Details
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  SKU & Quantity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Return Details
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status & Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {orders.map((order) => (
                <tr key={order.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{order.orderId}</div>
                    <div className="text-sm text-gray-500">{formatDate(order.orderDate)}</div>
                  </td>
                  
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{order.skuId}</div>
                    <div className="text-sm text-gray-500">Qty: {order.quantity}</div>
                  </td>
                  
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-red-600 font-medium">
                      {formatCurrency(order.returnAmount)}
                    </div>
                    <div className="text-sm text-gray-500">{order.orderStatus}</div>
                  </td>
                  
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getStatusBadgeClass(order.returnStatus)}`}>
                      {getStatusDisplayText(order.returnStatus)}
                    </span>
                    {order.receivedDate && (
                      <div className="text-sm text-gray-500 mt-1">
                        Received: {formatDate(order.receivedDate)}
                      </div>
                    )}
                  </td>
                  
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    {order.returnStatus === 'PENDING_RECEIPT' && (
                      <div className="flex space-x-2">
                        <button
                          onClick={() => openMarkReceivedModal(order)}
                          className="text-green-600 hover:text-green-900"
                        >
                          Mark Received
                        </button>
                        <button
                          onClick={() => openMarkNotReceivedModal(order)}
                          className="text-red-600 hover:text-red-900"
                        >
                          Mark Not Received
                        </button>
                      </div>
                    )}
                    {order.returnStatus === 'RECEIVED' && (
                      <div className="text-sm text-gray-500">
                        Received by: {order.receivedBy}
                      </div>
                    )}
                    {order.returnStatus === 'NOT_RECEIVED' && (
                      <div className="text-sm text-gray-500">
                        Notes: {order.notes || 'N/A'}
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* No Data State */}
      {!isLoading && !error && orders.length === 0 && (
        <div className="text-center py-8">
          <div className="text-gray-500 text-lg">No orders found for the selected criteria</div>
          <div className="text-gray-400 text-sm mt-2">
            Try adjusting your search filters or sync return orders
          </div>
        </div>
      )}

      {/* Mark Received Modal */}
      {showMarkReceivedModal && selectedOrder && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
            <div className="mt-3">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Mark Order as Received</h3>
              
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Order ID
                </label>
                <input
                  type="text"
                  value={selectedOrder.orderId}
                  disabled
                  className="px-3 py-2 border border-gray-300 rounded-md bg-gray-100 w-full"
                />
              </div>
              
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Received By *
                </label>
                <input
                  type="text"
                  value={receivedBy}
                  onChange={(e) => setReceivedBy(e.target.value)}
                  placeholder="Enter your name/ID"
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
                />
              </div>
              
              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Notes (Optional)
                </label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Any additional notes..."
                  rows={3}
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
                />
              </div>
              
              <div className="flex justify-end space-x-3">
                <button
                  onClick={() => setShowMarkReceivedModal(false)}
                  className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300"
                >
                  Cancel
                </button>
                <button
                  onClick={handleMarkAsReceived}
                  disabled={!receivedBy.trim() || isSubmitting}
                  className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
                >
                  {isSubmitting ? 'Marking...' : 'Mark as Received'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Mark Not Received Modal */}
      {showMarkNotReceivedModal && selectedOrder && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
            <div className="mt-3">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Mark Order as Not Received</h3>
              
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Order ID
                </label>
                <input
                  type="text"
                  value={selectedOrder.orderId}
                  disabled
                  className="px-3 py-2 border border-gray-300 rounded-md bg-gray-100 w-full"
                />
              </div>
              
              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Notes *
                </label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Reason why order was not received..."
                  rows={3}
                  className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
                />
              </div>
              
              <div className="flex justify-end space-x-3">
                <button
                  onClick={() => setShowMarkNotReceivedModal(false)}
                  className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300"
                >
                  Cancel
                </button>
                <button
                  onClick={handleMarkAsNotReceived}
                  disabled={!notes.trim() || isSubmitting}
                  className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50"
                >
                  {isSubmitting ? 'Marking...' : 'Mark as Not Received'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReturnTrackingTable;
