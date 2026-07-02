package com.jvmguard.agent.instrument.transaction;

public class DefinitionSite {
    private String definedFor;

    public DefinitionSite() {
    }

    public DefinitionSite(String definedFor) {
        this.definedFor = definedFor;
    }

    public DefinitionSite init(String definedFor) {
        this.definedFor = definedFor;
        return this;
    }

    public String getDefinedBy() {
        return definedFor;
    }

    public String getDefinedFor() {
        return definedFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefinitionSite that = (DefinitionSite)o;

        if (!definedFor.equals(that.definedFor)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return definedFor.hashCode();
    }

    @Override
    public String toString() {
        return "DefinitionSite{" +
            "definedFor='" + definedFor + '\'' +
            "}";
    }

    public static class AnnotationDefinitionSite extends DefinitionSite {
        private String definedBy;

        public AnnotationDefinitionSite() {
        }

        public AnnotationDefinitionSite(String definedFor, String definedBy) {
            super(definedFor);
            this.definedBy = definedBy;
        }

        public DefinitionSite init(String definedFor, String definedBy) {
            init(definedFor);
            this.definedBy = definedBy;
            return this;
        }

        @Override
        public String getDefinedBy() {
            return definedBy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            AnnotationDefinitionSite that = (AnnotationDefinitionSite)o;

            if (!definedBy.equals(that.definedBy)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + definedBy.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AnnotationDefinitionSite{" +
                "definedBy='" + definedBy + '\'' +
                "} " + super.toString();
        }
    }
}
