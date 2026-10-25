package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.server.component.DebugCardAccessContext;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.Optional;

public final class DebugCardData extends ItemData {
    private Optional<DebugCardAccessContext> access;

    public DebugCardData() {
        this(Optional.empty());
    }

    public DebugCardData(Option<DebugCardAccessContext> access) {
        this(OptionConverters.toJava(access));
    }

    public DebugCardData(Optional<DebugCardAccessContext> access) {
        super(Constants.ItemName$.MODULE$.DebugCard());
        this.access = access;
    }

    public DebugCardData(DataComponentHolder stack) {
        this();
        loadData(stack);
    }

    public Option<DebugCardAccessContext> access() { return OptionConverters.toScala(access); }

    public Optional<DebugCardAccessContext> accessOptional() { return access; }

    public void access_$eq(Option<DebugCardAccessContext> value) {
        access = OptionConverters.toJava(value);
    }

    public void accessOptional_$eq(Optional<DebugCardAccessContext> value) { access = value; }

    @Override
    public void loadData(DataComponentHolder holder) {
        access = DebugCardDataCodecs.loadOptional(holder);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        DebugCardDataCodecs.save(access, holder);
    }
}
