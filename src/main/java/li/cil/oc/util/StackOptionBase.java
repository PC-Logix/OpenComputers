package li.cil.oc.util;

import net.minecraft.world.item.ItemStack;
import scala.Function0;
import scala.Function1;
import scala.Option;
import scala.PartialFunction;
import scala.collection.Iterator;
import scala.collection.immutable.List;
import scala.util.Either;

import java.io.Serializable;

/** Shared operations for the Scala StackOption cases. */
public abstract class StackOptionBase implements scala.Product, Serializable {
    protected final ItemStack itemStack;

    protected StackOptionBase(ItemStack stack) {
        this.itemStack = stack;
    }

    public boolean isEmpty() { return StackOptionJava.isEmpty(itemStack); }
    public ItemStack get() { return itemStack; }
    public boolean isDefined() { return !isEmpty(); }
    public ItemStack getOrElse(ItemStack fallback) { return StackOptionJava.getOrElse(itemStack, fallback); }
    public ItemStack orEmpty() { return StackOptionJava.orEmpty(itemStack); }
    public StackOption map(Function1<ItemStack, ItemStack> f) { return StackOptionJava.map(itemStack, f); }
    public <B> B foldLazy(Function0<B> ifEmpty, Function1<ItemStack, B> f) { return StackOptionJava.fold(itemStack, ifEmpty, f); }
    public <B> Option<B> flatMap(Function1<ItemStack, Option<B>> f) { return StackOptionJava.flatMap(itemStack, f); }
    public StackOption filter(Function1<ItemStack, Object> predicate) { return StackOptionJava.filter(itemStack, (StackOption) this, predicate, false); }
    public StackOption filterNot(Function1<ItemStack, Object> predicate) { return StackOptionJava.filter(itemStack, (StackOption) this, predicate, true); }
    public boolean nonEmpty() { return isDefined(); }
    public boolean contains(Object elem) { return StackOptionJava.contains(itemStack, elem); }
    public boolean exists(Function1<ItemStack, Object> predicate) { return StackOptionJava.exists(itemStack, predicate); }
    public boolean forall(Function1<ItemStack, Object> predicate) { return StackOptionJava.forall(itemStack, predicate); }
    public <U> void foreach(Function1<ItemStack, U> f) { StackOptionJava.foreach(itemStack, f); }
    public <B> Option<B> collect(PartialFunction<ItemStack, B> pf) { return StackOptionJava.collect(itemStack, pf); }
    public StackOption orElseLazy(Function0<StackOption> alternative) { return StackOptionJava.orElse(itemStack, (StackOption) this, alternative); }
    public Iterator<ItemStack> iterator() { return StackOptionJava.iterator(itemStack); }
    public List<ItemStack> toList() { return StackOptionJava.toList(itemStack); }
    public <X> Either<X, ItemStack> toRightLazy(Function0<X> left) { return StackOptionJava.toRight(itemStack, left); }
    public <X> Either<ItemStack, X> toLeftLazy(Function0<X> right) { return StackOptionJava.toLeft(itemStack, right); }
    public Option<ItemStack> toOption() { return StackOptionJava.toOption(itemStack); }
}
