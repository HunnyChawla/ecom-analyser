import React, { useState, useEffect } from 'react';
import { 
  Plus, 
  Edit, 
  Trash2, 
  Package, 
  Users, 
  DollarSign, 
  TrendingUp,
  Search,
  X,
  Save,
  AlertCircle,
  Upload,
  Download
} from 'lucide-react';
import { api } from '../utils/api';

interface SkuGroup {
  id: number;
  groupName: string;
  purchasePrice: number;
  description: string;
  createdAt: string;
  skuCount?: number;
}

interface SkuMapping {
  id: number;
  skuId: string;
  groupName: string;
  groupId: number;
}

const EnhancedSkuGroupManagement: React.FC = () => {
  const [groups, setGroups] = useState<SkuGroup[]>([]);
  const [ungroupedSkus, setUngroupedSkus] = useState<string[]>([]);
  const [skuMappings, setSkuMappings] = useState<SkuMapping[]>([]);
  const [activeTab, setActiveTab] = useState<'groups' | 'skus' | 'ungrouped'>('groups');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  
  // Modal states
  const [showCreateGroupModal, setShowCreateGroupModal] = useState(false);
  const [showEditGroupModal, setShowEditGroupModal] = useState(false);
  const [showDeleteGroupModal, setShowDeleteGroupModal] = useState(false);
  const [showAddSkuModal, setShowAddSkuModal] = useState(false);
  const [showUpdateSkuModal, setShowUpdateSkuModal] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);
  
  // Form states
  const [newGroup, setNewGroup] = useState({
    groupName: '',
    purchasePrice: 0,
    description: ''
  });
  const [editGroup, setEditGroup] = useState({
    id: 0,
    groupName: '',
    purchasePrice: 0,
    description: ''
  });
  const [selectedSku, setSelectedSku] = useState<string>('');
  const [selectedSkuGroup, setSelectedSkuGroup] = useState<number>(0);
  const [selectedGroup, setSelectedGroup] = useState<SkuGroup | null>(null);
  
  // Upload states
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState('');
  const [uploadError, setUploadError] = useState('');

  useEffect(() => {
    fetchGroups();
    fetchUngroupedSkus();
    fetchSkuMappings();
  }, []);

  const fetchGroups = async () => {
    try {
      const response = await api.get('/api/sku-groups');
      setGroups(response.data);
    } catch (error) {
      console.error('Error fetching groups:', error);
      setGroups([]);
    }
  };

  const fetchUngroupedSkus = async () => {
    try {
      console.log('Fetching ungrouped SKUs...');
      const response = await api.get('/api/sku-groups/ungrouped');
      console.log('Ungrouped SKUs response:', response.data);
      const skus = Array.isArray(response.data?.ungroupedSkus) ? response.data.ungroupedSkus : [];
      console.log('Setting ungrouped SKUs:', skus);
      setUngroupedSkus(skus);
    } catch (error) {
      console.error('Error fetching ungrouped SKUs:', error);
      setUngroupedSkus([]);
    }
  };

  const fetchSkuMappings = async () => {
    try {
      const response = await api.get('/api/sku-groups/mappings');
      setSkuMappings(response.data || []);
    } catch (error) {
      console.error('Error fetching SKU mappings:', error);
      setSkuMappings([]);
    }
  };

  const handleCreateGroup = async () => {
    if (!newGroup.groupName.trim()) return;
    
    try {
      setLoading(true);
      await api.post('/api/sku-groups', newGroup);
      setNewGroup({ groupName: '', purchasePrice: 0, description: '' });
      setShowCreateGroupModal(false);
      fetchGroups();
    } catch (error) {
      console.error('Error creating group:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleEditGroup = async () => {
    if (!editGroup.groupName.trim()) return;
    
    try {
      setLoading(true);
      await api.put(`/api/sku-groups/${editGroup.id}`, editGroup);
      setShowEditGroupModal(false);
      fetchGroups();
    } catch (error) {
      console.error('Error updating group:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteGroup = async () => {
    if (!selectedGroup) return;
    
    try {
      setLoading(true);
      await api.delete(`/api/sku-groups/${selectedGroup.id}`);
      setShowDeleteGroupModal(false);
      setSelectedGroup(null);
      fetchGroups();
      fetchSkuMappings();
    } catch (error) {
      console.error('Error deleting group:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddSkuToGroup = async () => {
    if (!selectedSku || !selectedSkuGroup) return;
    
    try {
      setLoading(true);
      console.log('Adding SKU to group:', { selectedSku, selectedSkuGroup });
      
      // Debug: Check what api instance we're using
      console.log('API instance:', api);
      console.log('API baseURL:', api.defaults.baseURL);
      console.log('API interceptors:', api.interceptors);
      
      // Debug: Check token before request
      const token = localStorage.getItem('token');
      console.log('Token from localStorage:', token ? 'Present' : 'Missing');
      console.log('Token value:', token);
      
      const response = await api.post('/api/sku-groups/mappings', {
        skuId: selectedSku,
        groupId: selectedSkuGroup
      });
      
      console.log('Success response:', response.data);
      setSelectedSku('');
      setSelectedSkuGroup(0);
      setShowAddSkuModal(false);
      await fetchUngroupedSkus();
      await fetchSkuMappings();
    } catch (error: any) {
      console.error('Error adding SKU to group:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      console.error('Error config:', error.config);
      
      // Check if it's an authentication error
      if (error.response?.status === 401 || error.response?.status === 403) {
        console.error('Authentication/Authorization error detected');
        // Don't clear the form on auth errors
        return;
      }
      
      // For other errors, show user feedback
      alert(`Failed to add SKU to group: ${error.response?.data?.error || error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateSkuGroup = async () => {
    if (!selectedSku || !selectedSkuGroup) return;
    
    try {
      setLoading(true);
      console.log('Updating SKU group:', { selectedSku, selectedSkuGroup });
      
      // Debug: Check token before request
      const token = localStorage.getItem('token');
      console.log('Token from localStorage:', token ? 'Present' : 'Missing');
      
      const response = await api.put(`/api/sku-groups/mappings/${selectedSku}`, {
        groupId: selectedSkuGroup
      });
      
      console.log('Update success response:', response.data);
      setSelectedSku('');
      setSelectedSkuGroup(0);
      setShowUpdateSkuModal(false);
      await fetchSkuMappings();
    } catch (error: any) {
      console.error('Error updating SKU group:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      console.error('Error config:', error.config);
      
      // Check if it's an authentication error
      if (error.response?.status === 401 || error.response?.status === 403) {
        console.error('Authentication/Authorization error detected in update');
        return;
      }
      
      // For other errors, show user feedback
      alert(`Failed to update SKU group: ${error.response?.data?.error || error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const openEditGroupModal = (group: SkuGroup) => {
    setEditGroup({
      id: group.id,
      groupName: group.groupName,
      purchasePrice: group.purchasePrice,
      description: group.description
    });
    setShowEditGroupModal(true);
  };

  const openDeleteGroupModal = (group: SkuGroup) => {
    setSelectedGroup(group);
    setShowDeleteGroupModal(true);
  };

  const openAddSkuModal = () => {
    setShowAddSkuModal(true);
  };

  const openUpdateSkuModal = (skuId: string) => {
    setSelectedSku(skuId);
    setShowUpdateSkuModal(true);
  };

  const handleFileUpload = async () => {
    if (!uploadFile) return;
    
    setUploadLoading(true);
    setUploadError('');
    setUploadMessage('');
    
    const formData = new FormData();
    formData.append('file', uploadFile);
    
    try {
      const response = await api.post('/api/sku-groups/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      setUploadMessage(`Successfully imported ${response.data.importedGroups} groups`);
      setUploadFile(null);
      
      // Refresh data
      await fetchGroups();
      await fetchSkuMappings();
      
      // Close modal after a delay
      setTimeout(() => {
        setShowUploadModal(false);
        setUploadMessage('');
      }, 2000);
      
    } catch (error: any) {
      setUploadError(error.response?.data?.error || 'Upload failed');
    } finally {
      setUploadLoading(false);
    }
  };

  const downloadTemplate = async () => {
    try {
      const response = await api.get('/api/sku-groups/template', {
        responseType: 'blob',
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'sku_group_template.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading template:', error);
    }
  };

  const filteredGroups = groups.filter(group => 
    group.groupName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    group.description.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredSkuMappings = skuMappings.filter(mapping => 
    mapping.skuId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    mapping.groupName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">SKU Group Management</h1>
          <p className="text-gray-600">Manage SKU groups, assign SKUs, and track performance</p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowCreateGroupModal(true)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors flex items-center space-x-2"
          >
            <Plus className="w-4 h-4" />
            <span>Create Group</span>
          </button>
          <button
            onClick={openAddSkuModal}
            className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors flex items-center space-x-2"
          >
            <Package className="w-4 h-4" />
            <span>Add SKU to Group</span>
          </button>
          <button
            onClick={() => setShowUploadModal(true)}
            className="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700 transition-colors flex items-center space-x-2"
          >
            <Upload className="w-4 h-4" />
            <span>Upload Excel</span>
          </button>
          <button
            onClick={downloadTemplate}
            className="bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700 transition-colors flex items-center space-x-2"
          >
            <Download className="w-4 h-4" />
            <span>Download Template</span>
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center">
            <div className="p-2 bg-blue-100 rounded-lg">
              <Users className="w-5 h-5 text-blue-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Total Groups</p>
              <p className="text-2xl font-semibold text-gray-900">{groups.length}</p>
            </div>
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center">
            <div className="p-2 bg-green-100 rounded-lg">
              <Package className="w-5 h-5 text-green-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">SKU Mappings</p>
              <p className="text-2xl font-semibold text-gray-900">{skuMappings.length}</p>
            </div>
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center">
            <div className="p-2 bg-orange-100 rounded-lg">
              <Package className="w-5 h-5 text-orange-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Ungrouped SKUs</p>
              <p className="text-2xl font-semibold text-gray-900">{ungroupedSkus.length}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Debug Authentication Status */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-medium text-yellow-800">Debug: Authentication Status</h3>
            <p className="text-sm text-yellow-700 mt-1">
              Token: {localStorage.getItem('token') ? 'Present' : 'Missing'} | 
              User: {localStorage.getItem('user') ? 'Present' : 'Missing'}
            </p>
          </div>
          <button
            onClick={() => {
              console.log('Token:', localStorage.getItem('token'));
              console.log('User:', localStorage.getItem('user'));
              console.log('Current groups:', groups);
              console.log('Current ungrouped SKUs:', ungroupedSkus);
            }}
            className="text-xs bg-yellow-600 text-white px-2 py-1 rounded hover:bg-yellow-700 transition-colors"
          >
            Log Debug Info
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200">
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8 px-6">
            <button
              onClick={() => setActiveTab('groups')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'groups'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <div className="flex items-center space-x-2">
                <Users className="w-4 h-4" />
                <span>Groups ({groups.length})</span>
              </div>
            </button>
            <button
              onClick={() => setActiveTab('skus')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'skus'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <div className="flex items-center space-x-2">
                <Package className="w-4 h-4" />
                <span>SKU Mappings ({skuMappings.length})</span>
              </div>
            </button>
            <button
              onClick={() => setActiveTab('ungrouped')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'ungrouped'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <div className="flex items-center space-x-2">
                <Package className="w-4 h-4" />
                <span>Ungrouped SKUs ({ungroupedSkus.length})</span>
              </div>
            </button>
          </nav>
        </div>

        <div className="p-6">
          {/* Search Bar */}
          <div className="mb-6">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <input
                type="text"
                placeholder="Search groups, SKUs, or descriptions..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          {/* Groups Tab */}
          {activeTab === 'groups' && (
            <div className="space-y-4">
              {filteredGroups.map((group) => (
                <div key={group.id} className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3">
                        <h3 className="text-lg font-semibold text-gray-900">{group.groupName}</h3>
                        <span className="bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded-full">
                          {group.skuCount || 0} SKUs
                        </span>
                      </div>
                      <p className="text-gray-600 mt-1">{group.description}</p>
                      <div className="flex items-center space-x-4 mt-2 text-sm text-gray-500">
                        <span>Purchase Price: ₹{group.purchasePrice}</span>
                        <span>Created: {new Date(group.createdAt).toLocaleDateString()}</span>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => openEditGroupModal(group)}
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title="Edit Group"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => openDeleteGroupModal(group)}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete Group"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* SKU Mappings Tab */}
          {activeTab === 'skus' && (
            <div className="space-y-4">
              {filteredSkuMappings.map((mapping) => (
                <div key={mapping.id} className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3">
                        <span className="font-mono text-sm bg-gray-200 px-2 py-1 rounded">
                          {mapping.skuId}
                        </span>
                        <span className="text-gray-500">→</span>
                        <span className="font-semibold text-gray-900">{mapping.groupName}</span>
                      </div>
                    </div>
                    <button
                      onClick={() => openUpdateSkuModal(mapping.skuId)}
                      className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                      title="Update Group Assignment"
                    >
                      <Edit className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Ungrouped SKUs Tab */}
          {activeTab === 'ungrouped' && (
            <div className="space-y-4">
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="flex items-center">
                  <Package className="w-5 h-5 text-blue-600 mr-2" />
                  <div>
                    <h3 className="text-sm font-medium text-blue-800">
                      {ungroupedSkus.length} SKUs Available for Grouping
                    </h3>
                    <p className="text-sm text-blue-700 mt-1">
                      These SKUs are not assigned to any group. Use the "Add SKU to Group" button to assign them.
                    </p>
                  </div>
                </div>
              </div>
              
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                {ungroupedSkus.map((sku) => (
                  <div key={sku} className="bg-gray-50 rounded-lg p-3 border border-gray-200">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium text-gray-900 truncate">{sku}</span>
                      <button
                        onClick={() => {
                          setSelectedSku(sku);
                          setShowAddSkuModal(true);
                        }}
                        className="text-xs bg-blue-600 text-white px-2 py-1 rounded hover:bg-blue-700 transition-colors"
                      >
                        Add to Group
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Create Group Modal */}
      {showCreateGroupModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Create New Group</h3>
              <button
                onClick={() => setShowCreateGroupModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Group Name</label>
                <input
                  type="text"
                  value={newGroup.groupName}
                  onChange={(e) => setNewGroup({ ...newGroup, groupName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Enter group name"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Purchase Price</label>
                <input
                  type="number"
                  step="0.01"
                  value={newGroup.purchasePrice}
                  onChange={(e) => setNewGroup({ ...newGroup, purchasePrice: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="0.00"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={newGroup.description}
                  onChange={(e) => setNewGroup({ ...newGroup, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  rows={3}
                  placeholder="Enter group description"
                />
              </div>
              <div className="flex space-x-3 pt-4">
                <button
                  onClick={() => setShowCreateGroupModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={handleCreateGroup}
                  disabled={loading || !newGroup.groupName.trim()}
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  {loading ? 'Creating...' : 'Create Group'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Edit Group Modal */}
      {showEditGroupModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Edit Group</h3>
              <button
                onClick={() => setShowEditGroupModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Group Name</label>
                <input
                  type="text"
                  value={editGroup.groupName}
                  onChange={(e) => setEditGroup({ ...editGroup, groupName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Purchase Price</label>
                <input
                  type="number"
                  step="0.01"
                  value={editGroup.purchasePrice}
                  onChange={(e) => setEditGroup({ ...editGroup, purchasePrice: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={editGroup.description}
                  onChange={(e) => setEditGroup({ ...editGroup, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  rows={3}
                />
              </div>
              <div className="flex space-x-3 pt-4">
                <button
                  onClick={() => setShowEditGroupModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={handleEditGroup}
                  disabled={loading || !editGroup.groupName.trim()}
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  {loading ? 'Updating...' : 'Update Group'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delete Group Modal */}
      {showDeleteGroupModal && selectedGroup && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center space-x-3 mb-4">
              <AlertCircle className="w-6 h-6 text-red-500" />
              <h3 className="text-lg font-semibold text-gray-900">Delete Group</h3>
            </div>
            <p className="text-gray-600 mb-6">
              Are you sure you want to delete the group "{selectedGroup.groupName}"? 
              This action cannot be undone and will remove all SKU associations.
            </p>
            <div className="flex space-x-3">
              <button
                onClick={() => setShowDeleteGroupModal(false)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteGroup}
                disabled={loading}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {loading ? 'Deleting...' : 'Delete Group'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add SKU to Group Modal */}
      {showAddSkuModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Add SKU to Group</h3>
              <button
                onClick={() => setShowAddSkuModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Select SKU</label>
                <select
                  value={selectedSku}
                  onChange={(e) => setSelectedSku(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">Choose a SKU</option>
                  {ungroupedSkus.map((sku) => (
                    <option key={sku} value={sku}>{sku}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Select Group</label>
                <select
                  value={selectedSkuGroup}
                  onChange={(e) => setSelectedSkuGroup(parseInt(e.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value={0}>Choose a group</option>
                  {groups.map((group) => (
                    <option key={group.id} value={group.id}>{group.groupName}</option>
                  ))}
                </select>
              </div>
              <div className="flex space-x-3 pt-4">
                <button
                  onClick={() => setShowAddSkuModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={handleAddSkuToGroup}
                  disabled={loading || !selectedSku || !selectedSkuGroup}
                  className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                >
                  {loading ? 'Adding...' : 'Add SKU to Group'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Update SKU Group Modal */}
      {showUpdateSkuModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Update SKU Group</h3>
              <button
                onClick={() => setShowUpdateSkuModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">SKU ID</label>
                <input
                  type="text"
                  value={selectedSku}
                  disabled
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Group</label>
                <select
                  value={selectedSkuGroup}
                  onChange={(e) => setSelectedSkuGroup(parseInt(e.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value={0}>Choose a group</option>
                  {groups.map((group) => (
                    <option key={group.id} value={group.id}>{group.groupName}</option>
                  ))}
                </select>
              </div>
              <div className="flex space-x-3 pt-4">
                <button
                  onClick={() => setShowUpdateSkuModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={handleUpdateSkuGroup}
                  disabled={loading || !selectedSkuGroup}
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  {loading ? 'Updating...' : 'Update Group'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Upload Excel Modal */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Upload SKU Groups Excel</h3>
              <button
                onClick={() => setShowUploadModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Excel File</label>
                <input
                  type="file"
                  accept=".xlsx,.xls"
                  onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">
                  Upload Excel file with columns: Group Name, SKU, Purchase Price, Description
                </p>
              </div>
              
              {uploadMessage && (
                <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                  <p className="text-green-800 text-sm">{uploadMessage}</p>
                </div>
              )}
              
              {uploadError && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                  <p className="text-red-800 text-sm">{uploadError}</p>
                </div>
              )}
              
              <div className="flex space-x-3 pt-4">
                <button
                  onClick={() => setShowUploadModal(false)}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={handleFileUpload}
                  disabled={uploadLoading || !uploadFile}
                  className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50"
                >
                  {uploadLoading ? 'Uploading...' : 'Upload File'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EnhancedSkuGroupManagement;
