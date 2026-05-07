package cloudsim;

public class Constants {
    public static final int NO_OF_VMS = 10;
    public static final int NO_OF_TASKS = 50;
    
    // VM Parameters
    public static final long VM_SIZE = 10000;
    public static final int VM_RAM = 512;
    public static final long VM_BW = 1000;
    public static final int VM_PES = 1;

    // Migration delay model: fraction of VM_BW used for task migration
    // migration_delay (s) = cloudlet_length / (VM_BW * MIGRATION_BW_FACTOR)
    public static final double MIGRATION_BW_FACTOR = 0.1;
    public static final String VM_VMM = "Xen";
    
    // Cloudlet Parameters
    public static final long CLOUDLET_FILE_SIZE = 300;
    public static final long CLOUDLET_OUTPUT_SIZE = 300;
    public static final int CLOUDLET_PES = 1;
}
