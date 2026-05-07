import matplotlib.pyplot as plt
import os
import sys

def create_graphs():
    algorithms = ['FCFS', 'SJF', 'Min-Min', 'Proposed DLB']
    makespan = [103.46, 114.83, 53.53, 48.52]
    arur = [48.0, 43.99, 81.01, 93.38]

    # Colors for the bars
    colors = ['#FF9999', '#66B2FF', '#99FF99', '#FFCC99']

    # 1. Makespan Graph
    plt.figure(figsize=(8, 5))
    bars1 = plt.bar(algorithms, makespan, color=colors, edgecolor='black')
    plt.title('Makespan Comparison', fontsize=14, fontweight='bold')
    plt.ylabel('Makespan Time (seconds)', fontsize=12)
    plt.xlabel('Scheduling Algorithm', fontsize=12)
    
    # Add values on top of bars
    for bar in bars1:
        yval = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2, yval + 1, round(yval, 2), ha='center', va='bottom', fontsize=10)

    # Save to desktop project folder
    desktop_path = r"C:\Users\sarth\OneDrive\Desktop\CloudSimProject"
    if not os.path.exists(desktop_path):
        os.makedirs(desktop_path, exist_ok=True)
        
    makespan_path = os.path.join(desktop_path, 'Makespan_Comparison.png')
    plt.savefig(makespan_path, dpi=300, bbox_inches='tight')
    plt.close()

    # 2. ARUR Graph
    plt.figure(figsize=(8, 5))
    bars2 = plt.bar(algorithms, arur, color=colors, edgecolor='black')
    plt.title('Average Resource Utilization Ratio (ARUR)', fontsize=14, fontweight='bold')
    plt.ylabel('ARUR (%)', fontsize=12)
    plt.xlabel('Scheduling Algorithm', fontsize=12)
    plt.ylim(0, 110) # Set Y limit to a bit over 100 for visual padding
    
    # Add values on top of bars
    for bar in bars2:
        yval = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2, yval + 1, str(round(yval, 2)) + '%', ha='center', va='bottom', fontsize=10)

    arur_path = os.path.join(desktop_path, 'ARUR_Comparison.png')
    plt.savefig(arur_path, dpi=300, bbox_inches='tight')
    plt.close()

    print(f"Graphs successfully saved to {desktop_path}")

if __name__ == "__main__":
    try:
        create_graphs()
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)
