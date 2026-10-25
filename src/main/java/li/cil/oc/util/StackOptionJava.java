package li.cil.oc.util;

import net.minecraft.world.item.ItemStack;
import scala.Function0;
import scala.Function1;
import scala.Option;
import scala.PartialFunction;
import scala.collection.Iterator;
import scala.collection.Iterator$;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;

/** Java implementation of the ItemStack option operations used by the Scala ADT. */
public final class StackOptionJava {
    public static boolean isEmpty(ItemStack stack) { return stack == null || stack.isEmpty(); }
    public static ItemStack getOrElse(ItemStack stack, ItemStack fallback) { return isEmpty(stack) ? fallback : stack; }
    public static ItemStack orEmpty(ItemStack stack) { return isEmpty(stack) ? ItemStack.EMPTY : stack; }
    public static StackOption map(ItemStack stack, Function1<ItemStack, ItemStack> f) {
        return isEmpty(stack) ? StackOption.empty() : StackOption.apply(f.apply(stack));
    }
    public static <B> B fold(ItemStack stack, Function0<B> ifEmpty, Function1<ItemStack, B> f) {
        return isEmpty(stack) ? ifEmpty.apply() : f.apply(stack);
    }
    @SuppressWarnings("unchecked")
    public static <B> Option<B> flatMap(ItemStack stack, Function1<ItemStack, Option<B>> f) {
        return isEmpty(stack) ? (Option<B>) (Option<?>) scala.None$.MODULE$ : f.apply(stack);
    }
    public static StackOption filter(ItemStack stack, StackOption self, Function1<ItemStack, Object> predicate, boolean negate) {
        if (isEmpty(stack)) return self;
        boolean matches = (Boolean) predicate.apply(stack);
        return (negate ? !matches : matches) ? self : StackOption.empty();
    }
    public static boolean contains(ItemStack stack, Object elem) { return !isEmpty(stack) && stack.equals(elem); }
    public static boolean exists(ItemStack stack, Function1<ItemStack, Object> predicate) { return !isEmpty(stack) && (Boolean) predicate.apply(stack); }
    public static boolean forall(ItemStack stack, Function1<ItemStack, Object> predicate) { return isEmpty(stack) || (Boolean) predicate.apply(stack); }
    public static <U> void foreach(ItemStack stack, Function1<ItemStack, U> f) { if (!isEmpty(stack)) f.apply(stack); }
    @SuppressWarnings("unchecked")
    public static <B> Option<B> collect(ItemStack stack, PartialFunction<ItemStack, B> pf) { return isEmpty(stack) ? (Option<B>) (Option<?>) scala.None$.MODULE$ : pf.lift().apply(stack); }
    public static StackOption orElse(ItemStack stack, StackOption self, Function0<StackOption> alternative) { return isEmpty(stack) ? alternative.apply() : self; }
    public static Iterator<ItemStack> iterator(ItemStack stack) { return isEmpty(stack) ? Iterator$.MODULE$.empty() : Iterator$.MODULE$.single(stack); }
    public static List<ItemStack> toList(ItemStack stack) { return isEmpty(stack) ? List$.MODULE$.empty() : new scala.collection.immutable.$colon$colon<>(stack, List$.MODULE$.empty()); }
    public static <X> Either<X, ItemStack> toRight(ItemStack stack, Function0<X> left) { return isEmpty(stack) ? new Left<>(left.apply()) : new Right<>(stack); }
    public static <X> Either<ItemStack, X> toLeft(ItemStack stack, Function0<X> right) { return isEmpty(stack) ? new Right<>(right.apply()) : new Left<>(stack); }
    @SuppressWarnings("unchecked")
    public static Option<ItemStack> toOption(ItemStack stack) { return isEmpty(stack) ? (Option<ItemStack>) (Option<?>) scala.None$.MODULE$ : Option.apply(stack); }

    private StackOptionJava() {}
}
