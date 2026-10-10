import net.sweenus.simplybows.util.RuneEffectRules;
import net.sweenus.simplybows.upgrade.RuneEtching;
import java.util.*;

/** Checks complete cast schedules, reload continuation and reachable upgrade budgets. */
public class RuneFormationRegression {
    public static void main(String[] args) {
        require(RuneEffectRules.damage(RuneEffectRules.VORTEX,0)==4, "unupgraded water volley must deal four damage");
        require(RuneEffectRules.finisher(0)==6, "unupgraded water final volley must deal six damage");
        for (int strings=0;strings<=5;strings++) for (int frames=0;frames+strings<=5;frames++) {
            int swarmDuration=RuneEffectRules.duration(RuneEffectRules.SWARM, strings);
            List<Integer> stings=events(RuneEffectRules.SWARM, strings, 0);
            require(stings.size()==5+strings, "a String must add exactly one sting");
            require(stings.get(stings.size()-1)==swarmDuration, "last sting must occur before cleanup");
            double swarm=stings.size()*RuneEffectRules.damage(RuneEffectRules.SWARM, frames);
            require(swarm<=28.01, "five-slot swarm budget exceeds intended ceiling");
            List<Integer> vortex=events(RuneEffectRules.VORTEX, strings, 0);
            require(vortex.size()>=4 && vortex.size()<=6, "vortex pulse count is outside budget");
            double water=vortex.size()*RuneEffectRules.damage(RuneEffectRules.VORTEX,frames)+RuneEffectRules.finisher(frames);
            require(water<=35.01, "five-slot water ability budget exceeds intended ceiling");
            require(RuneEffectRules.cooldown(RuneEffectRules.VORTEX,strings)>RuneEffectRules.duration(RuneEffectRules.VORTEX,strings), "vortex must have downtime");
            for (int kind:new int[]{RuneEffectRules.SWARM,RuneEffectRules.VORTEX}) {
                List<Integer> complete=events(kind,strings,0);
                for (int saved:new int[]{12,20,40,60,80}) {
                    List<Integer> continued=events(kind,strings,saved);
                    List<Integer> expected=new ArrayList<>();
                    for (int event:complete) if(event>saved) expected.add(event);
                    require(continued.equals(expected), "resumed cast must neither replay nor omit later pulses");
                }
            }
        }
        Set<Integer> closingTicks=new TreeSet<>();
        for(int pair=0;pair<RuneEffectRules.STONE_PAIRS;pair++) {
            int strike=RuneEffectRules.stoneStrikeTick(pair);
            require(closingTicks.add(strike),"stone pairs must not all close at once");
            require(RuneEffectRules.stoneClosing(strike-10,pair)==0,"pair closes before its windup");
            require(RuneEffectRules.stoneClosing(strike,pair)==1,"damage precedes full closure");
            require(strike<RuneEffectRules.duration(RuneEffectRules.STONE,0),"last pair closes after cleanup");
            if(pair>0) require(RuneEffectRules.stonePairZ(pair,3)>RuneEffectRules.stonePairZ(pair-1,3),"pairs must advance in order");
        }
        require(closingTicks.equals(Set.of(28,36,44,52,60)),"unexpected crushing sequence");
        Set<Integer> waves=new HashSet<>();
        for(int tick=1;tick<=RuneEffectRules.duration(RuneEffectRules.STAR,0);tick++)
            if(RuneEffectRules.starWaveAge(tick)==0) waves.add(tick);
        require(waves.equals(Set.of(1,31,61)), "Bounty must keep exactly three outward waves");
        for(int ray=0;ray<8;ray++) {
            double angle=ray*Math.PI/4;
            require(RuneEffectRules.insideStar(Math.cos(angle)*6,Math.sin(angle)*6,6,.001), "star misses an outer tip");
            double between=angle+Math.PI/8;
            require(RuneEffectRules.insideStar(Math.cos(between)*2,Math.sin(between)*2,6,0), "star interior must be filled between rays");
            require(!RuneEffectRules.insideStar(Math.cos(between)*6,Math.sin(between)*6,6,0), "star must preserve its indented outline");
        }
        for(int strings=0;strings<=5;strings++) {
            var cells=RuneEffectRules.starCells(strings);
            require(cells.size()>90,"star still looks like eight sparse lines");
            Set<String> positions=new HashSet<>();
            for(var cell:cells) {
                require(positions.add(cell.x()+":"+cell.z()),"duplicate geometry cell");
                require(cell.fraction()<=1.000001,"cell lies outside filled star");
            }
        }
        require(RuneEffectRules.starProgress(0)==0 && RuneEffectRules.starProgress(15)==1 && RuneEffectRules.starProgress(30)==0,"each wave must expand and return");
        for(int phase=1;phase<15;phase++) {
            require(RuneEffectRules.starProgress(phase)>RuneEffectRules.starProgress(phase-1),"outward phase reverses early");
            require(RuneEffectRules.starProgress(30-phase)==RuneEffectRules.starProgress(phase),"inward phase differs from outward phase");
        }
        for(int kind=0;kind<=4;kind++) {
            int duration=RuneEffectRules.duration(kind,0), end=duration+RuneEffectRules.FINISH_TICKS;
            require(RuneEffectRules.animationScale(duration,end)==1,"effect fades before final damage");
            float previous=1;
            for(int t=duration+1;t<=end;t++) {
                float scale=RuneEffectRules.animationScale(t,end);
                require(scale<previous && scale>=0,"finish must decrease smoothly");previous=scale;
                require(!RuneEffectRules.pulseTick(kind,t,0),"finish animation must not add damage pulses");
            }
            require(previous==0,"effect must be invisible before removal");
        }
        require(RuneEffectRules.animationScale(0,-1)==0 && RuneEffectRules.animationScale(6,-1)==1,"effect needs a growth animation");
        require(RuneEffectRules.level(-1)==0 && RuneEffectRules.level(99)==5,"invalid saved upgrades must clamp");
        require(RuneEffectRules.kind("ice",RuneEtching.PAIN)==-1,"Winterfang must retain its existing Pain ability");
        require(RuneEffectRules.kind("earth",RuneEtching.GRACE)==-1,"Grace must retain wall support logic");
        require(RuneEffectRules.kind("bubble",RuneEtching.PAIN)==RuneEffectRules.VORTEX,"wrong rune routing");
        System.out.println("Passed: pulse schedules, resumed casts, five-slot damage budgets, three filled-star out/in cycles, damage-free finish animations and rune routing.");
    }
    static List<Integer> events(int kind,int strings,int saved) {
        List<Integer> result=new ArrayList<>();
        for(int t=saved+1;t<=RuneEffectRules.duration(kind,strings);t++) if(RuneEffectRules.pulseTick(kind,t,strings)) result.add(t);
        return result;
    }
    static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
