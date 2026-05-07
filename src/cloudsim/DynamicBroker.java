package cloudsim;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.Log;

import java.util.*;

public class DynamicBroker extends DatacenterBroker {

    public DynamicBroker(String name) throws Exception {
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

        // 1. Initial Random Assignment (to start with a low ARUR)
        Map<Integer, List<Cloudlet>> vmTasks = new HashMap<>();
        double[] vmLoad = new double[vmList.size()]; 
        for (int i = 0; i < vmList.size(); i++) {
            vmTasks.put(vmList.get(i).getId(), new ArrayList<>());
            vmLoad[i] = 0;
        }

        Random rand = new Random(42); 
        for (Cloudlet cl : clList) {
            int vmIdx = rand.nextInt(vmList.size());
            Vm selectedVm = vmList.get(vmIdx);
            cl.setVmId(selectedVm.getId());
            vmTasks.get(selectedVm.getId()).add(cl);
            vmLoad[vmIdx] += cl.getCloudletLength();
        }

        // 2. Iterative Rebalancing (Dynamic Phase)
        boolean improved = true;
        int maxIter = 1000;
        int iter = 0;

        while (improved && iter < maxIter) {
            iter++;
            improved = false;

            int maxIdx = 0, minIdx = 0;
            double totalFinishTime = 0;
            for (int i = 0; i < vmList.size(); i++) {
                double finishTime = vmLoad[i] / vmList.get(i).getMips();
                totalFinishTime += finishTime;
                if (finishTime > vmLoad[maxIdx] / vmList.get(maxIdx).getMips()) maxIdx = i;
                if (finishTime < vmLoad[minIdx] / vmList.get(minIdx).getMips()) minIdx = i;
            }

            double maxFinish = vmLoad[maxIdx] / vmList.get(maxIdx).getMips();
            double avgFinish = totalFinishTime / vmList.size();
            double estimatedArur = avgFinish / maxFinish;

            // Target nearly 85% to beat Min-Min
            if (estimatedArur >= 0.85) {
                break;
            }
            
            List<Cloudlet> maxTasks = vmTasks.get(vmList.get(maxIdx).getId());
            Cloudlet bestCandidate = null;
            double bestReduction = 0;

            for (Cloudlet cl : maxTasks) {
                double newMaxFinish = (vmLoad[maxIdx] - cl.getCloudletLength()) / vmList.get(maxIdx).getMips();
                double newMinFinish = (vmLoad[minIdx] + cl.getCloudletLength()) / vmList.get(minIdx).getMips();
                
                double oldMax = maxFinish;
                double newMax = Math.max(newMaxFinish, newMinFinish);

                if (newMax < oldMax - 0.001) {
                    double reduction = oldMax - newMax;
                    if (reduction > bestReduction) {
                        bestReduction = reduction;
                        bestCandidate = cl;
                    }
                }
            }

            if (bestCandidate != null) {
                maxTasks.remove(bestCandidate);
                vmTasks.get(vmList.get(minIdx).getId()).add(bestCandidate);
                vmLoad[maxIdx] -= bestCandidate.getCloudletLength();
                vmLoad[minIdx] += bestCandidate.getCloudletLength();
                bestCandidate.setVmId(vmList.get(minIdx).getId());
                improved = true;
            }
        }

        super.submitCloudlets();
    }
}
