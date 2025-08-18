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
  AlertCircle
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
  const [activeTab, setActiveTab] = useState<'groups' | 'skus'>('groups');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  
  // Modal states
  const [showCreateGroupModal, setShowCreateGroupModal] = useState(false);
  const [showEditGroupModal, setShowEditGroupModal] = useState(false);
  const [showDeleteGroupModal, setShowDeleteGroupModal] = useState(false);
  const [showAddSkuModal, setShowAddSkuModal] = useState(false);
  const [showUpdateSkuModal, setShowUpdateSkuModal] = useState(false);
  
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
      const response = await api.get('/api/sku-groups/ungrouped');
      setUngroupedSkus(Array.isArray(response.data?.ungroupedSkus) ? response.data.ungroupedSkus : []);
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
      await api.post('/api/sku-groups/mappings', {
        skuId: selectedSku,
        groupId: selectedSkuGroup
      });
      setSelectedSku('');
      setSelectedSkuGroup(0);
      setShowAddSkuModal(false);
      fetchUngroupedSkus();
      fetchSkuMappings();
    } catch (error) {
      console.error('Error adding SKU to group:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateSkuGroup = async () => {
    if (!selectedSku || !selectedSkuGroup) return;
    
    try {
      setLoading(true);
      await api.put(`/api/sku-groups/mappings/${selectedSku}`, {
        groupId: selectedSkuGroup
      });
      setSelectedSku('');
      setSelectedSkuGroup(0);
      setShowUpdateSkuModal(false);
      fetchSkuMappings();
    } catch (error) {
      console.error('Error updating SKU group:', error);
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
    </div>
  );
};

export default EnhancedSkuGroupManagement;
