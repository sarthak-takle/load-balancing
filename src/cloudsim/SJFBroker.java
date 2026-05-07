package cloudsim;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import java.util.Comparator;
import java.util.List;

public class SJFBroker extends DatacenterBroker {

    public SJFBroker(String name) throws Exception {
        super(name);
    }

    @Override
    protected void submitCloudlets() {
        List<Cloudlet> clList = getCloudletList();
        List<Vm> vmList = getVmsCreatedList();

        if (clList == null || clList.isEmpty() || vmList == null || vmList.isEmpty()) {
            super.submitCloudlets();
            return;
        }

        // Sort Cloudlets ascending by length (Shortest Job First)
        clList.sort(Comparator.comparingDouble(Cloudlet::getCloudletLength));

        // Assign sequentially
        int vmIndex = 0;
        for (Cloudlet cl : clList) {
            Vm vm = vmList.get(vmIndex);
            cl.setVmId(vm.getId());
            vmIndex = (vmIndex + 1) % vmList.size();
        }

        super.submitCloudlets();
    }
}
