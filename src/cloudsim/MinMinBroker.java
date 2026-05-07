package cloudsim;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinMinBroker extends DatacenterBroker {

    public MinMinBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void submitCloudlets() {
        List<Cloudlet> clList = new ArrayList<>(getCloudletList());
        List<Vm> vmList = getVmsCreatedList();

        if (clList == null || clList.isEmpty() || vmList == null || vmList.isEmpty()) {
            super.submitCloudlets();
            return;
        }

        Map<Integer, Double> readyTime = new HashMap<>();
        for (Vm vm : vmList) {
            readyTime.put(vm.getId(), 0.0);
        }

        // Min-Min Assignment
        while (!clList.isEmpty()) {
            double minGlobalTime = Double.MAX_VALUE;
            Cloudlet selectedCloudlet = null;
            Vm selectedVm = null;

            for (Cloudlet cl : clList) {
                for (Vm vm : vmList) {
                    double executionTime = cl.getCloudletLength() / vm.getMips();
                    double completionTime = readyTime.get(vm.getId()) + executionTime;

                    if (completionTime < minGlobalTime) {
                        minGlobalTime = completionTime;
                        selectedCloudlet = cl;
                        selectedVm = vm;
                    }
                }
            }

            if (selectedCloudlet != null && selectedVm != null) {
                selectedCloudlet.setVmId(selectedVm.getId());
                readyTime.put(selectedVm.getId(), minGlobalTime);
                clList.remove(selectedCloudlet);
            } else {
                break; // Failsafe
            }
        }

        super.submitCloudlets();
    }
}
