"""
Correction orthographique via SymSpell.
Utilise un dictionnaire de fréquences téléchargé automatiquement.
"""
import logging
import os
import urllib.request
from symspellpy import SymSpell, Verbosity

logger = logging.getLogger(__name__)

_sym_spell: SymSpell | None = None

DICT_URL = (
    "https://raw.githubusercontent.com/wolfgarbe/SymSpell/master/"
    "SymSpell.FrequencyDictionary/fr-100k.txt"
)
DICT_PATH = "/tmp/symspell_dict_fr.txt"


def load_symspell():
    global _sym_spell
    _sym_spell = SymSpell(max_dictionary_edit_distance=2, prefix_length=7)

    if not os.path.exists(DICT_PATH):
        logger.info("Téléchargement du dictionnaire SymSpell...")
        try:
            urllib.request.urlretrieve(DICT_URL, DICT_PATH)
            logger.info("Dictionnaire téléchargé.")
        except Exception as exc:
            logger.warning("Impossible de télécharger le dictionnaire : %s", exc)
            return

    loaded = _sym_spell.load_dictionary(DICT_PATH, term_index=0, count_index=1)
    if loaded:
        logger.info("SymSpell chargé.")
    else:
        logger.warning("Échec du chargement du dictionnaire SymSpell.")


def correct_query(query: str) -> str:
    """Corrige les fautes de frappe dans une requête."""
    if _sym_spell is None:
        return query
    suggestions = _sym_spell.lookup_compound(query, max_edit_distance=2)
    if suggestions:
        corrected = suggestions[0].term
        return corrected if corrected != query else query
    return query
