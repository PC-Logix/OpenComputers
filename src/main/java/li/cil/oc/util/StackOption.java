package li.cil.oc.util;

import net.minecraft.world.item.ItemStack;
import scala.Function0;
import scala.Function1;
import scala.Option;
import scala.$less$colon$less;
import scala.runtime.AbstractFunction1;
import scala.util.Either;

/** Optional item stack with Scala collection-compatible operations. */
public abstract class StackOption extends StackOptionBase {
    public static final StackOption EmptyStack = new Empty();

    protected StackOption(ItemStack stack) {
        super(stack);
    }

    public static StackOption apply(ItemStack stack) {
        return stack == null || stack.isEmpty() ? EmptyStack : new SomeStack(stack);
    }

    public static StackOption apply(Option<ItemStack> stack) {
        return stack == null || stack.isEmpty() || stack.get().isEmpty() ? EmptyStack : new SomeStack(stack.get());
    }

    public static StackOption empty() {
        return EmptyStack;
    }

    public <B> B fold(Function0<B> ifEmpty, Function1<ItemStack, B> f) {
        return foldLazy(ifEmpty, f);
    }

    @SuppressWarnings("unchecked")
    public <B> Option<B> flatten($less$colon$less<ItemStack, Option<B>> ev) {
        return isEmpty() ? (Option<B>) (Option<?>) scala.None$.MODULE$ : ev.apply(get());
    }

    public StackOption orElse(Function0<StackOption> alternative) {
        return orElseLazy(alternative);
    }

    public <X> Either<X, ItemStack> toRight(Function0<X> left) {
        return toRightLazy(left);
    }

    public <X> Either<ItemStack, X> toLeft(Function0<X> right) {
        return toLeftLazy(right);
    }

    public WithFilter withFilter(Function1<ItemStack, Object> predicate) {
        return new WithFilter(predicate);
    }

    public final class WithFilter {
        private final Function1<ItemStack, Object> predicate;

        private WithFilter(Function1<ItemStack, Object> predicate) {
            this.predicate = predicate;
        }

        public <U> void foreach(Function1<ItemStack, U> f) {
            if (!isEmpty() && Boolean.TRUE.equals(predicate.apply(get()))) f.apply(get());
        }

        public WithFilter withFilter(Function1<ItemStack, Object> other) {
            return new WithFilter(new AbstractFunction1<>() {
                @Override
                public Object apply(ItemStack stack) {
                    return Boolean.TRUE.equals(predicate.apply(stack)) && Boolean.TRUE.equals(other.apply(stack));
                }
            });
        }
    }

    private static final class Empty extends StackOption {
        private Empty() { super(ItemStack.EMPTY); }
        @Override public int productArity() { return 0; }
        @Override public Object productElement(int index) { throw new IndexOutOfBoundsException(index); }
        @Override public String productPrefix() { return "EmptyStack"; }
        @Override public boolean canEqual(Object other) { return other instanceof Empty; }
        @Override public boolean equals(Object other) { return other instanceof Empty; }
        @Override public int hashCode() { return 0; }
        @Override public String toString() { return "EmptyStack"; }
    }

    public static final class SomeStack extends StackOption {
        private final ItemStack stack;
        public SomeStack(ItemStack stack) { super(stack); this.stack = stack; }
        public ItemStack stack() { return stack; }
        @Override public int productArity() { return 1; }
        @Override public Object productElement(int index) {
            if (index == 0) return stack;
            throw new IndexOutOfBoundsException(index);
        }
        @Override public String productPrefix() { return "SomeStack"; }
        @Override public boolean canEqual(Object other) { return other instanceof SomeStack; }
        @Override public boolean equals(Object other) {
            return other instanceof SomeStack that && stack.equals(that.stack);
        }
        @Override public int hashCode() { return stack.hashCode(); }
        @Override public String toString() { return "SomeStack(" + stack + ")"; }
    }
}
