package li.cil.oc.util;

/** Mutable fields are populated directly by Gson when reading GitHub release metadata. */
public final class UpdateRelease {
    public String tag_name = "";
    public String body = "";
    public boolean prerelease;
}
