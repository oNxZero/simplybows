Run with Java 21 or newer:

```sh
javac -d /tmp/simplybows-balance-check common/src/main/java/net/sweenus/simplybows/upgrade/RuneEtching.java common/src/main/java/net/sweenus/simplybows/util/BowAbilityBalance.java tests/ability-balance/BalanceRegression.java
java -cp /tmp/simplybows-balance-check BalanceRegression
```

Checks damage reductions including external ability bonuses, Frame scaling, and exact wave range extension for different step distances. Does not launch Minecraft.
