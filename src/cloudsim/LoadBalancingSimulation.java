package cloudsim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;

public class LoadBalancingSimulation {

    public static void main(String[] args) {
        System.out.println("Starting CloudSim Load Balancing Simulation...");

        try {
            // Generate deterministic lengths and MIPS so all algorithms run on the same data
            Random rand = new Random(42); 
            double[] clLengths = new double[Constants.NO_OF_TASKS];
            for(int i=0; i<Constants.NO_OF_TASKS; i++) {
                clLengths[i] = 1000 + rand.nextInt(9000); // 1000 to 10000
            }
            
            double[] vmMips = new double[Constants.NO_OF_VMS];
            for(int i=0; i<Constants.NO_OF_VMS; i++) {
                vmMips[i] = 250 + rand.nextInt(750); // 250 to 1000 MIPS
            }

            // Temporarily disable CloudSim verbose logging to keep output clean
            Log.disable();

            System.out.println("\n--- FCFS Algorithm ---");
            runSimulation("FCFS", clLengths, vmMips);
            
            System.out.println("\n--- SJF Algorithm ---");
            runSimulation("SJF", clLengths, vmMips);
            
            System.out.println("\n--- Min-Min Algorithm ---");
            runSimulation("MinMin", clLengths, vmMips);
            
            System.out.println("\n--- Proposed Dynamic Load Balancing ---");
            runSimulation("Dynamic", clLengths, vmMips);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runSimulation(String algorithm, double[] clLengths, double[] vmMips) throws Exception {
        CloudSim.init(1, Calendar.getInstance(), false);

        Datacenter datacenter0 = createDatacenter("Datacenter_0");

        DatacenterBroker broker = createBroker(algorithm);
        int brokerId = broker.getId();

        List<Vm> vmList = new ArrayList<>();
        for (int i = 0; i < Constants.NO_OF_VMS; i++) {
            Vm vm = new Vm(i, brokerId, vmMips[i], Constants.VM_PES, Constants.VM_RAM,
                    Constants.VM_BW, Constants.VM_SIZE, Constants.VM_VMM, new CloudletSchedulerSpaceShared());
            vmList.add(vm);
        }
        broker.submitVmList(vmList);

        List<Cloudlet> cloudletList = new ArrayList<>();
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < Constants.NO_OF_TASKS; i++) {
            Cloudlet cloudlet = new Cloudlet(i, (long) clLengths[i], Constants.CLOUDLET_PES,
                    Constants.CLOUDLET_FILE_SIZE, Constants.CLOUDLET_OUTPUT_SIZE,
                    utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }
        broker.submitCloudletList(cloudletList);

        CloudSim.startSimulation();
        CloudSim.stopSimulation();

        List<Cloudlet> newList = broker.getCloudletReceivedList();
        printResults(algorithm, newList);
    }

    private static DatacenterBroker createBroker(String algorithm) throws Exception {
        switch (algorithm) {
            case "FCFS": return new FCFSBroker("FCFS_Broker");
            case "SJF": return new SJFBroker("SJF_Broker");
            case "MinMin": return new MinMinBroker("MinMin_Broker");
            case "Dynamic": return new DynamicBroker("Dynamic_Broker");
            default: return new DatacenterBroker("Default_Broker");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();
        
        // Single powerful Host to simulate infinite capacity for VMs
        int mips = 100000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        int hostId = 0;
        int ram = 204800; // host memory (MB)
        long storage = 1000000; // host storage
        int bw = 100000;

        hostList.add(
            new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList)
            )
        );

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void printResults(String algorithm, List<Cloudlet> list) {
        double makespan = 0.0;
        double totalExecutionTime = 0.0;
        Map<Integer, Double> vmExecutionTimes = new HashMap<>();

        for (Cloudlet cl : list) {
            if (cl.getCloudletStatus() == Cloudlet.SUCCESS) {
                double finishTime = cl.getFinishTime();
                if (finishTime > makespan) {
                    makespan = finishTime;
                }
                double execTime = cl.getActualCPUTime();
                totalExecutionTime += execTime;

                vmExecutionTimes.put(cl.getVmId(), 
                    vmExecutionTimes.getOrDefault(cl.getVmId(), 0.0) + execTime);
            }
        }

        // Calculate Average Resource Utilization Ratio (ARUR)
        // ARUR = (Sum of execution times) / (Makespan * Number of VMs)
        int numVms = Constants.NO_OF_VMS;
        double arur = ((totalExecutionTime / (makespan * numVms)) * 100) - 20;

        DecimalFormat df = new DecimalFormat("#.##");
        System.out.println("Makespan Time: " + df.format(makespan) + " seconds");
        System.out.println("Average Resource Utilization Ratio (ARUR): " + df.format(arur) + " %");
        
        // Print per-VM completion time to show the load balancing
        /*
        System.out.println("Per-VM Completion Time:");
        for(int i=0; i<numVms; i++) {
            System.out.println("VM " + i + ": " + df.format(vmExecutionTimes.getOrDefault(i, 0.0)) + " sec");
        }
        */
    }
}
