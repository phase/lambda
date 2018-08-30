# Lambda Calculus Interpreter

<s>It is currently untyped. It may get types in the future.</s>

Currently in the process of getting types. (System F; Polymorphic Lambda Calculus) 

Here's a preview of what is currently supported:

```
>> 0 := \f.\x.x
0 : /a./b./c./d.((a -> b) -> ((c -> d) -> (c -> d))) = \f.\x.x
```

```
0 := \f.\x.x
1 := \f.\x.f x
2 := \f.\x.f (f x)
3 := \f.\x.f (f (f x))

true  := \x.\y.x
false := \x.\y.y
and := \p.\q. p q p
or  := \p.\q. p p q
not := \p.p false true
if := \p.\a.\b.p a b
isZero := \n.n (\x.false) true

inc := \n.\f.\x.f (n f x)
dec := \n.\f.\x.n (\g.\h.h (g f)) (\u.x) (\u.u)

plus := \m.\n.m inc n
mult := \m.\n.m (+ n) 0
sub  := \m.\n.n (dec m)

(mult (add 2 3)) 2
```
