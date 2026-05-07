package cloudsim;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
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

        // ── 1. Initial Random Assignment ──────────────────────────────────────
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

        // ── 2. Iterative Rebalancing with Metrics Tracking ───────────────────
        boolean improved = true;
        int maxIter = 1000;
        int iter = 0;

        // Metrics accumulators
        int totalMigrations = 0;
        double totalMigrationDelay = 0.0;
        long totalSelectionTimeNs = 0;
        int selectionCount = 0;

        DecimalFormat df3 = new DecimalFormat("0.000");
        DecimalFormat df4 = new DecimalFormat("0.0000");

        // All log lines collected, then written to file
        List<String> logLines = new ArrayList<>();
        logLines.add("DLBA Migration Log");
        logLines.add("==========================================");

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

            if (estimatedArur >= 0.85) {
                break;
            }

            List<Cloudlet> maxTasks = vmTasks.get(vmList.get(maxIdx).getId());
            Cloudlet bestCandidate = null;
            double bestReduction = 0;

            // ── Time the candidate search ─────────────────────────────────────
            long selectionStart = System.nanoTime();

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

            long selectionEndNs = System.nanoTime() - selectionStart;
            totalSelectionTimeNs += selectionEndNs;
            selectionCount++;
            double selectionMs = selectionEndNs / 1_000_000.0;

            // ── Perform migration if a candidate was found ────────────────────
            if (bestCandidate != null) {
                // Simulated migration delay: task_length / (BW * factor)
                double migrationDelay = bestCandidate.getCloudletLength()
                        / (Constants.VM_BW * Constants.MIGRATION_BW_FACTOR);

                maxTasks.remove(bestCandidate);
                vmTasks.get(vmList.get(minIdx).getId()).add(bestCandidate);
                vmLoad[maxIdx] -= bestCandidate.getCloudletLength();
                vmLoad[minIdx] += bestCandidate.getCloudletLength();
                bestCandidate.setVmId(vmList.get(minIdx).getId());
                improved = true;

                totalMigrations++;
                totalMigrationDelay += migrationDelay;

                // Format log line for this iteration
                String line = String.format(
                    "[DLBA] Iter %-4d | Task #%-3d (len=%-6d) migrated | Selection: %s ms | Delay: %s s",
                    iter,
                    bestCandidate.getCloudletId(),
                    bestCandidate.getCloudletLength(),
                    df3.format(selectionMs),
                    df4.format(migrationDelay)
                );
                logLines.add(line);
                System.out.println(line);
            }
        }

        // ── Summary ───────────────────────────────────────────────────────────
        double avgSelectionMs = selectionCount > 0
                ? (totalSelectionTimeNs / (double) selectionCount) / 1_000_000.0
                : 0.0;

        List<String> summaryLines = new ArrayList<>();
        summaryLines.add("");
        summaryLines.add("[DLBA] ── Migration Summary ──────────────────────────────────");
        summaryLines.add(String.format("[DLBA]   Total iterations run        : %d", iter));
        summaryLines.add(String.format("[DLBA]   Total migrations performed  : %d", totalMigrations));
        summaryLines.add(String.format("[DLBA]   Total migration delay       : %s s", df4.format(totalMigrationDelay)));
        summaryLines.add(String.format("[DLBA]   Avg task selection time     : %s ms", df3.format(avgSelectionMs)));
        summaryLines.add("[DLBA] ─────────────────────────────────────────────────────");

        for (String s : summaryLines) {
            logLines.add(s);
            System.out.println(s);
        }

        // ── Write to file ─────────────────────────────────────────────────────
        String logFile = "dlba_migration_log.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
            for (String line : logLines) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("[DLBA] Log written to: " + logFile);
        } catch (IOException e) {
            System.err.println("[DLBA] Failed to write log file: " + e.getMessage());
        }

        super.submitCloudlets();
    }
}
