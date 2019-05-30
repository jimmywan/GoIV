package com.kamron.pogoiv.scanlogic;


import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.util.SparseArray;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.kamron.pogoiv.GoIVSettings;
import com.kamron.pogoiv.R;
import lombok.AllArgsConstructor;
import timber.log.Timber;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Johan Swanberg on 2016-08-18.
 * A class which interprets pokemon information
 */
public class PokeInfoCalculator {

    // Some Pokedex numbers
    public static final int EEVEE = 133;
    public static final int VAPOREON = 134;
    public static final int JOLTEON = 135;
    public static final int FLAREON = 136;
    public static final int ESPEON = 196;
    public static final int UMBREON = 197;
    public static final int LEAFEON = 470;
    public static final int GLACEON = 471;
    public static final int MARILL = 183;
    public static final int AZURILL = 298;
    public static final int NIDORAN_FEMALE = 29;
    public static final int NIDORAN_MALE = 32;

    @AllArgsConstructor
    public static class TranslatedType {

        public final Pokemon.Type internalType;
        public final String name;

    }

    private static PokeInfoCalculator instance;

    private final List<PokemonBase> pokedex;
    private final List<Pokemon> formVariantPokemons;
    private final String[] pokeNamesWithForm;
    private final List<TranslatedType> types;

    /**
     * Pokemons who's name appears as a type of candy.
     * For most, this is the basePokemon (ie: Pidgey candies)
     * For some, this is an original Gen1 Pokemon (ie: Magmar candies, instead of Magby candies)
     */
    private final List<PokemonBase> candyPokemons;

    // Representation of each pokemon form entry in the JSON file
    private static class PokemonEntry {
        int baseAttack;
        int baseDefense;
        int baseStamina;
        String[] types;
        String form;
        List<String> devolution;

        transient Pokemon object;

        public Pokemon create(BaseEntry base) {
            Pokemon.Type[] types = new Pokemon.Type[this.types.length];
            for (int i = 0; i < this.types.length; i++) {
                types[i] = Pokemon.Type.valueOf(this.types[i]);
            }
            object = new Pokemon(base.object, base.names.getForm(form), form, baseAttack, baseDefense, baseStamina);
            base.object.forms.add(object);
            return object;
        }
    }

    // Representation of each pokemon base entry in the JSON file
    private static class BaseEntry {
        int number;
        String family;
        String name;
        Integer devolution;
        PokemonEntry[] forms;
        int candy;

        transient PokemonBase object;
        transient Names names;

        public PokemonBase create(Names names, boolean hasMultiple) {
            object = new PokemonBase(name, names.getName(number), names.getDisplayName(number), number, candy, hasMultiple);
            this.names = names;
            return object;
        }

        public List<Pokemon> createForms() {
            List<Pokemon> formObjects = new ArrayList<>(forms.length);
            for (PokemonEntry form : forms) {
                formObjects.add(form.create(this));
            }
            return formObjects;
        }
    }

    private static class NameEntry {
        Map<Integer, String> names;
        Map<String, String> forms;
        Map<String, String> types;
    }

    private static class Names {

        private final Map<Integer, String> names;
        private final Map<Integer, String> displayNames;
        private final Map<String, String> forms;
        private final Map<String, TranslatedType> types;

        public Names(@NonNull GoIVSettings settings, @NonNull Context context) {
            NameEntry defaults = loadNames(context, "en");
            if (defaults == null) {
                throw new IllegalArgumentException("Couldn't load default names");
            }
            boolean useDefaultsOCR = context.getResources().getBoolean(R.bool.use_default_pokemonsname_as_ocrstring);
            // Make a copy of each map in the translation, in case we actually have a translation. They would overwrite
            // the default values, which would actually overwrite the default entries too.
            NameEntry translation = new NameEntry();
            translation.names = new HashMap<>(defaults.names);
            translation.forms = new HashMap<>(defaults.forms);
            translation.types = new HashMap<>(defaults.types);
            if (!useDefaultsOCR || settings.isShowTranslatedPokemonName()) {
                NameEntry actualNames = loadNames(context);
                if (actualNames != null) {
                    translation.names.putAll(actualNames.names);
                    translation.forms.putAll(actualNames.forms);
                    translation.types.putAll(actualNames.types);
                }
            }
            names = (useDefaultsOCR ? defaults : translation).names;
            displayNames = (settings.isShowTranslatedPokemonName() ? translation : defaults).names;
            forms = translation.forms;
            types = new HashMap<>();
            for (String internalName : defaults.types.keySet()) {
                types.put(internalName, new TranslatedType(Pokemon.Type.valueOf(internalName), translation.types.get(internalName)));
            }
        }

        private static NameEntry loadNames(@NonNull Context context) {
            Locale locale = getLocale(context.getResources());
            NameEntry names = loadNames(context, locale.getLanguage() + "-r" + locale.getCountry());
            if (names != null) {
                return names;
            }
            return loadNames(context, locale.getLanguage());
        }

        private static NameEntry loadNames(@NonNull Context context, @NonNull String language) {
            JsonReader jsonReader;
            try {
                jsonReader = new JsonReader(
                        new InputStreamReader(context.getAssets().open("pokemons/" + language + "/names.json")));
            } catch (IOException e) {
                Timber.e(e);
                return null;
            }
            return new Gson().fromJson(jsonReader, NameEntry.class);
        }

        public String getName(int number) {
            return names.get(number);
        }

        public String getDisplayName(int number) {
            return displayNames.get(number);
        }

        public String getForm(String form) {
            return forms.get(form);
        }

        /*public String getType(String type) {
            return types.get(type);
        }*/
    }

    private static Locale getLocale(@NonNull Resources res) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return res.getConfiguration().getLocales().get(0);
        } else {
            return res.getConfiguration().locale;
        }
    }

    protected static synchronized @NonNull PokeInfoCalculator getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new PokeInfoCalculator(GoIVSettings.getInstance(context), context);
        }
        return instance;
    }

    /**
     * Get the instance of pokeInfoCalculator. Must have been initiated first!
     *
     * @return the already activated instance of PokeInfoCalculator.
     */
    public static @Nullable PokeInfoCalculator getInstance() {
        return instance;
    }

    /**
     * Creates a pokemon info calculator with the pokemon as argument.
     *
     * @param settings Settings instance
     * @param context The context of this instance
     */
    private PokeInfoCalculator(@NonNull GoIVSettings settings, @NonNull Context context) {
        JsonReader json;
        try {
            json = new JsonReader(new InputStreamReader(context.getAssets().open("pokemons/goiv-stats.json")));
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't load the pokemon stats file");
        }

        Names names = new Names(settings, context);
        BaseEntry[] entries = new Gson().fromJson(json, BaseEntry[].class);
        SparseArray<PokemonBase> numberedPokedex = new SparseArray<>();
        HashSet<String> multipleForms = new HashSet<>();
        int formCount = 0;
        // First determine which families have multiple forms, in those cases we want to show all forms
        for (BaseEntry entry : entries) {
            if (entry.forms.length > 1) {
                multipleForms.add(entry.family);
            }
            formCount += entry.forms.length;
        }
        ArrayList<Pokemon> formVariantPokemons = new ArrayList<>(formCount);
        ArrayList<PokemonBase> pokedex = new ArrayList<>(entries.length);
        ArrayList<PokemonBase> candyPokemons = new ArrayList<>();

        // Now create each entry base entry, their forms and assign the "candy" names
        for (BaseEntry entry : entries) {
            PokemonBase base = entry.create(names, multipleForms.contains(entry.family));
            numberedPokedex.put(base.number, base);

            if (entry.family.equals(entry.name)) {
                candyPokemons.add(base);
            }

            formVariantPokemons.addAll(entry.createForms());
        }

        // Now go through each item and assign the devolutions, also add the item to the evolution in the devolutions.
        for (BaseEntry baseEntry : entries) {
            if (baseEntry.devolution != null) {
                baseEntry.object.devolution = numberedPokedex.get(baseEntry.devolution);
                baseEntry.object.devolution.evolutions.add(baseEntry.object);

                for (PokemonEntry form : baseEntry.forms) {
                    for (Pokemon devolvedForm : baseEntry.object.devolution.forms) {
                        if (form.devolution.contains(devolvedForm.internalFormName)) {
                            form.object.devolutions.add(devolvedForm);
                            devolvedForm.evolutions.add(form.object);
                        }
                    }
                }
            }
            pokedex.add(baseEntry.object);
        }

        this.pokedex = Collections.unmodifiableList(pokedex);
        this.formVariantPokemons = Collections.unmodifiableList(formVariantPokemons);
        this.types = Collections.unmodifiableList(new ArrayList<>(names.types.values()));
        this.pokeNamesWithForm = new String[this.formVariantPokemons.size()];
        for (int i = 0; i < this.formVariantPokemons.size(); i++) {
            this.pokeNamesWithForm[i] = this.formVariantPokemons.get(i).toString();
        }
        this.candyPokemons = Collections.unmodifiableList(candyPokemons);
    }

    public List<PokemonBase> getPokedex() {
        return pokedex;
    }

    public List<TranslatedType> getTypes() {
        return types;
    }

    public PokemonBase get(int number) {
        for (PokemonBase base : pokedex) {
            if (base.number == number) {
                return base;
            }
        }
        return null;
    }

    public List<Pokemon> getPokedexForms() {
        return formVariantPokemons;
    }

    /**
     * Returns the full list of names possible candy name.
     *
     * @return List of all candy names that exist in Pokemon Go.
     */
    public List<PokemonBase> getCandyPokemons() {
        return candyPokemons;
    }

    /**
     * Return the full pokemon display names list, including forms.
     *
     * @return the full pokemon display names including forms as string array.
     */
    public String[] getPokemonNamesWithFormArray() {
        return pokeNamesWithForm;
    }

    /**
     * Gets the needed required candy and stardust to hit max level (relative to trainer level).
     *
     * @param goalLevel             The level to reach
     * @param estimatedPokemonLevel The estimated level of the pokemon
     * @param isLucky               Whether the pokemon is lucky, therefore costs one half normal dust
     * @return The text that shows the amount of candy and stardust needed.
     */
    public UpgradeCost getUpgradeCost(double goalLevel, double estimatedPokemonLevel, boolean isLucky) {
        int neededCandy = 0;
        int neededStarDust = 0;
        while (estimatedPokemonLevel != goalLevel) {
            int rank = 5;
            if ((estimatedPokemonLevel % 10) >= 1 && (estimatedPokemonLevel % 10) <= 2.5) {
                rank = 1;
            } else if ((estimatedPokemonLevel % 10) > 2.5 && (estimatedPokemonLevel % 10) <= 4.5) {
                rank = 2;
            } else if ((estimatedPokemonLevel % 10) > 4.5 && (estimatedPokemonLevel % 10) <= 6.5) {
                rank = 3;
            } else if ((estimatedPokemonLevel % 10) > 6.5 && (estimatedPokemonLevel % 10) <= 8.5) {
                rank = 4;
            }

            if (estimatedPokemonLevel <= 10.5) {
                neededCandy++;
                neededStarDust += rank * 200;
            } else if (estimatedPokemonLevel > 10.5 && estimatedPokemonLevel <= 20.5) {
                neededCandy += 2;
                neededStarDust += 1000 + (rank * 300);
            } else if (estimatedPokemonLevel > 20.5 && estimatedPokemonLevel <= 25.5) {
                neededCandy += 3;
                neededStarDust += 2500 + (rank * 500);
            } else if (estimatedPokemonLevel > 25.5 && estimatedPokemonLevel <= 30.5) {
                neededCandy += 4;
                neededStarDust += 2500 + (rank * 500);
            } else if (estimatedPokemonLevel > 30.5 && estimatedPokemonLevel <= 32.5) {
                neededCandy += 6;
                neededStarDust += 5000 + (rank * 1000);
            } else if (estimatedPokemonLevel > 32.5 && estimatedPokemonLevel <= 34.5) {
                neededCandy += 8;
                neededStarDust += 5000 + (rank * 1000);
            } else if (estimatedPokemonLevel > 34.5 && estimatedPokemonLevel <= 36.5) {
                neededCandy += 10;
                neededStarDust += 5000 + (rank * 1000);
            } else if (estimatedPokemonLevel > 36.5 && estimatedPokemonLevel <= 38.5) {
                neededCandy += 12;
                neededStarDust += 5000 + (rank * 1000);
            } else if (estimatedPokemonLevel > 38.5) {
                neededCandy += 15;
                neededStarDust += 5000 + (rank * 1000);
            }

            estimatedPokemonLevel += 0.5;
        }

        if (isLucky) {
            neededStarDust /= 2;
        }

        return new UpgradeCost(neededStarDust, neededCandy);
    }


    /**
     * Calculates all the IV information that can be gained from the pokemon level, hp and cp
     * and fills the information in an ScanResult.
     *
     * @param scanResult Pokefly scan results
     */
    public void getIVPossibilities(ScanResult scanResult) {
        scanResult.clearIVCombinations();

        if (scanResult.levelRange.min == scanResult.levelRange.max) {
            getSingleLevelIVPossibility(scanResult, scanResult.levelRange.min);
        }

        for (double i = scanResult.levelRange.min; i <= scanResult.levelRange.max; i += 0.5) {
            getSingleLevelIVPossibility(scanResult, i);
        }
    }

    /**
     * Calculates all the IV information that can be gained from the pokemon level, hp and cp
     * and fills the information in an ScanResult.
     *
     * @param scanResult Pokefly scan results
     *                   many possibilities.
     */
    private void getSingleLevelIVPossibility(ScanResult scanResult, double level) {
        int baseAttack = scanResult.pokemon.baseAttack;
        int baseDefense = scanResult.pokemon.baseDefense;
        int baseStamina = scanResult.pokemon.baseStamina;

        double lvlScalar = Data.getLevelCpM(level);
        double lvlScalarPow2 = Math.pow(lvlScalar, 2) * 0.1; // instead of computing again in every loop
        //IV vars for lower and upper end cp ranges

        for (int staminaIV = 0; staminaIV < 16; staminaIV++) {
            int hp = (int) Math.max(Math.floor((baseStamina + staminaIV) * lvlScalar), 10);
            if (hp == scanResult.hp) {
                double lvlScalarStamina = Math.sqrt(baseStamina + staminaIV) * lvlScalarPow2;
                for (int defenseIV = 0; defenseIV < 16; defenseIV++) {
                    for (int attackIV = 0; attackIV < 16; attackIV++) {
                        int cp = Math.max(10, (int) Math.floor((baseAttack + attackIV) * Math.sqrt(baseDefense
                                + defenseIV) * lvlScalarStamina));
                        if (cp == scanResult.cp) {
                            scanResult.addIVCombination(attackIV, defenseIV, staminaIV);
                        }
                    }
                }
            } else if (hp > scanResult.hp) {
                break;
            }
        }
    }


    /**
     * getCpAtRangeLeve
     * Used to calculate CP ranges for a species at a specific level based on the lowest and highest
     * IV combination.
     * <p/>
     * Returns a string on the form of "\n CP at lvl X: A - B" where x is the pokemon level, A is minCP and B is maxCP
     *
     * @param pokemon the index of the pokemon species within the pokemon list (sorted)
     * @param low     combination of lowest IVs
     * @param high    combination of highest IVs
     * @param level   pokemon level for CP calculation
     * @return CPrange containing the CP range including the specified level.
     */
    public CPRange getCpRangeAtLevel(Pokemon pokemon, IVCombination low, IVCombination high, double level) {
        if (low == null || high == null || level < 0 || pokemon == null) {
            return new CPRange(0, 0);
        }
        int baseAttack = pokemon.baseAttack;
        int baseDefense = pokemon.baseDefense;
        int baseStamina = pokemon.baseStamina;
        double lvlScalar = Data.getLevelCpM(level);
        int cpMin = (int) Math.floor(
                (baseAttack + low.att) * Math.sqrt(baseDefense + low.def) * Math.sqrt(baseStamina + low.sta)
                        * Math.pow(lvlScalar, 2) * 0.1);
        int cpMax = (int) Math.floor((baseAttack + high.att) * Math.sqrt(baseDefense + high.def)
                * Math.sqrt(baseStamina + high.sta) * Math.pow(lvlScalar, 2) * 0.1);
        if (cpMin > cpMax) {
            int tmp = cpMax;
            cpMax = cpMin;
            cpMin = tmp;
        }
        return new CPRange(cpMin, cpMax);
    }

    /**
     * Get the combined cost for evolving all steps between two pokemon, for example the cost from caterpie ->
     * metapod is 12,
     * caterpie -> butterfly is 12+50 = 62.
     *
     * @param start which pokemon to start from
     * @param end   the end evolution
     * @return the combined candy cost for all required evolutions
     */
    public int getCandyCostForEvolution(Pokemon start, Pokemon end) {
        int cost = 0;
        PokemonBase currentBase = end.base;
        // Iterate over each devolution until there is none OR the current devolution has the start pokemon
        while (!currentBase.forms.contains(start)) {
            currentBase = end.base.devolution;
            if (currentBase == null) {
                return 0;
            }
            cost += currentBase.candyEvolutionCost;
        }
        return cost;
    }

    /**
     * Check if two pokemon are in the same complete evolution chain. Jolteon and vaporeon would return true
     *
     * @param p1 first pokemon
     * @param p2 second pokemon
     * @return true if both pokemon are in the same pokemon evolution tree
     */
    private boolean isInSameEvolutionChain(Pokemon p1, Pokemon p2) {
        ArrayList<PokemonBase> evolutionLine = getEvolutionLine(p1.base);
        for (PokemonBase base : evolutionLine) {
            if (base.number == p2.base.number) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the lowest evolution in the chain of a pokemon.
     *
     * @param base a pokemon, example charizard
     * @return a pokemon, in the example would return charmander
     */
    private PokemonBase getLowestEvolution(PokemonBase base) {
        while (base.devolution != null) {
            base = base.devolution;
        }
        return base;
    }

    /**
     * Returns all forms of all evolutions belonging to the pokemon.
     *
     * @param pokemon the pokemon to check the evolution line of
     * @return a list with pokemon, devolutions and evolutions and forms.
     */
    public ArrayList<Pokemon> getEvolutionForms(Pokemon pokemon) {
        ArrayList<Pokemon> list = new ArrayList<>();

        for (PokemonBase base : getEvolutionLine(pokemon.base)) {
            list.addAll(base.forms);
        }

        return list;
    }

    /**
     * Returns the evolution line of a pokemon.
     *
     * @param poke the pokemon to check the evolution line of
     * @return a list with pokemon, input pokemon plus its (d)evolutions
     */
    public ArrayList<Pokemon> getEvolutionLine(Pokemon poke) {
        return poke.getEvolutionLine();
    }

    /**
     * Returns the evolution line of a pokemon.
     *
     * @param base the pokemon to check the evolution line of
     * @return a list with pokemon, input pokemon plus its evolutions
     */
    public ArrayList<PokemonBase> getEvolutionLine(PokemonBase base) {
        base = getLowestEvolution(base);

        ArrayList<PokemonBase> list = new ArrayList<>();
        list.add(base);
        for (PokemonBase evolution2nd : base.evolutions) {
            list.add(evolution2nd);
            for (PokemonBase evolution3rd : evolution2nd.evolutions) {
                list.add(evolution3rd);
            }
        }

        return list;
    }

    /**
     * Get how much hp a pokemon will have at a certain level, including the stamina IV taken from the scan results.
     * If the prediction is not exact because of big possible variation in stamina IV, the average will be returnred.
     *
     * @param scanResult      Scan results which includes stamina ivs
     * @param selectedLevel   which level to get the hp for
     * @param selectedPokemon Which pokemon to get Hp for
     * @return An integer representing how much hp selectedpokemon with ivscanresult stamina ivs has at selectedlevel
     */
    public int getHPAtLevel(ScanResult scanResult, double selectedLevel, Pokemon selectedPokemon) {
        double lvlScalar = Data.getLevelCpM(selectedLevel);
        int highHp = (int) Math.max(
                Math.floor((selectedPokemon.baseStamina + scanResult.getIVStaminaHigh()) * lvlScalar), 10);
        int lowHp = (int) Math.max(
                Math.floor((selectedPokemon.baseStamina + scanResult.getIVStaminaLow()) * lvlScalar), 10);
        return Math.round((highHp + lowHp) / 2f);
    }
}
