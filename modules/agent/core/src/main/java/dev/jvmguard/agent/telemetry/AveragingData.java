package dev.jvmguard.agent.telemetry;

import java.util.Arrays;

class AveragingData {
    long[] currentValues;
    long[] averageSum;
    int[] averageDivisor;
    long[] average;
    int[] multiplier;

    public AveragingData(int length) {
        currentValues = new long[length];
        averageDivisor = new int[length];
        averageSum = new long[length];
        average = new long[length];
        multiplier = new int[length];
        Arrays.fill(multiplier, 1);
    }

    public long[] getAverage() {
        for (int i = 0; i < currentValues.length; i++) {
            if (averageDivisor[i] == 0) {
                average[i] = Long.MIN_VALUE;
            } else {
                average[i] = averageSum[i] * multiplier[i] / averageDivisor[i];
            }
            averageSum[i] = 0;
            averageDivisor[i] = 0;
        }
        return average;
    }

    public void addAverage() {
        for (int i = 0; i < currentValues.length; i++) {
            if (currentValues[i] > Long.MIN_VALUE) {
                averageDivisor[i]++;
                averageSum[i] += currentValues[i];
            }
        }
    }

    @Override
    public String toString() {
        return "Data{" +
            "currentValues=" + Arrays.toString(currentValues) +
            ", averageSum=" + Arrays.toString(averageSum) +
            ", averageDivisor=" + Arrays.toString(averageDivisor) +
            ", average=" + Arrays.toString(average) +
            "}";
    }
}
