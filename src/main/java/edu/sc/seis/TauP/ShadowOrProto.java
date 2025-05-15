package edu.sc.seis.TauP;

public class ShadowOrProto {

    public ShadowOrProto(ProtoSeismicPhase proto) {
        this.proto = proto;
    }
    public ShadowOrProto(ShadowZone shadow) {
        this.shadow = shadow;
    }

    public boolean isProto() {
        return proto != null;
    }

    public boolean isShadow() {
        return shadow != null;
    }

    public ProtoSeismicPhase getProto() {
        return proto;
    }

    public ShadowZone getShadow() {
        return shadow;
    }

    ProtoSeismicPhase proto = null;
    ShadowZone shadow = null;
}
