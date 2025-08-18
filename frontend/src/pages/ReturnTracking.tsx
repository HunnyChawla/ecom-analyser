import React from 'react';
import ReturnTrackingTable from '../components/ReturnTrackingTable';

const ReturnTracking: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Return Tracking</h1>
          <p className="text-gray-600">
            Track and manage return orders with comprehensive status monitoring. 
            Sync return orders from the system, mark them as received or not received, 
            and maintain detailed records of the return process.
          </p>
        </div>
        <ReturnTrackingTable />
      </div>
    </div>
  );
};

export default ReturnTracking;
