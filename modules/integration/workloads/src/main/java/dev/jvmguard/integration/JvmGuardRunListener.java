package dev.jvmguard.integration;

import dev.jvmguard.agent.instrument.Transformer;
import dev.jvmguard.agent.instrument.Transformer.RetransformListener;
import dev.jvmguard.agent.parameter.ConfigurationParameter;
import dev.jvmguard.agent.parameter.ConfigurationParameter.RecordingOptionsListener;

import java.util.Collection;

public class JvmGuardRunListener {
    public static void initListener(AbstractJvmGuardRun abstractJvmGuardRun) {
        ConfigurationParameter.recordingOptionsListener = new MyRecordingOptionsListener(abstractJvmGuardRun);
        Transformer.retransformListener = new MyRetransformListener(abstractJvmGuardRun);
    }

    static class MyRecordingOptionsListener implements RecordingOptionsListener {
        AbstractJvmGuardRun abstractJvmGuardRun;

        MyRecordingOptionsListener(AbstractJvmGuardRun abstractJvmGuardRun) {
            this.abstractJvmGuardRun = abstractJvmGuardRun;
        }

        @Override
        public void preSet(ConfigurationParameter parameters) {
        }

        @Override
        public void postSet(ConfigurationParameter parameters) {
            if (AbstractJvmGuardRun.currentConfigurationNumber++ == 0) {
                new Thread(() -> {
                    System.out.println("WORK");
                    try {
                        abstractJvmGuardRun.work();
                    } catch (Throwable t) {
                        t.printStackTrace();
                        System.exit(1);
                    }
                }).start();
            }
        }
    }

    static class MyRetransformListener implements RetransformListener {
        AbstractJvmGuardRun abstractJvmGuardRun;

        MyRetransformListener(AbstractJvmGuardRun abstractJvmGuardRun) {
            this.abstractJvmGuardRun = abstractJvmGuardRun;
        }

        @Override
        public void retransform(Collection<Class> classes) {
            int nextConfig = AbstractJvmGuardRun.currentConfigurationNumber + 1;
            boolean recordMode = "yes".equals(System.getenv("JVMGUARD_RECORD"));

            StringBuilder builder = new StringBuilder();
            if (recordMode) {
                builder.append("RECORD MODE ");
            }
            builder.append("CONFIG ").append(nextConfig).append(" RETRANSFORMING {");
            for (Class clazz : classes) {
                builder.append('"').append(clazz.getName()).append("\",");
            }
            if (builder.charAt(builder.length() - 1) == ',') {
                builder.setLength(builder.length() - 1);
            }
            builder.append("}");

            System.out.println(builder);
            try {
                if (!recordMode) {
                    if (nextConfig == 1) {
                        if (!classes.isEmpty()) {
                            System.out.println("initial retransformation not as expected");
                            System.exit(1);
                        }
                    } else if (!abstractJvmGuardRun.checkRetransform(nextConfig, classes)) {
                        System.out.println("retransformation not as expected");
                        System.exit(1);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace(System.out);
                System.out.println("retransformation not as expected");
                System.exit(1);
            }
        }
    }

}
