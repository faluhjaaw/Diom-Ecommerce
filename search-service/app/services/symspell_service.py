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

TECH_WORDS = {
    'laptop', 'smartphone', 'iphone', 'samsung', 'macbook', 'airpods',
    'gaming', 'pc', 'wifi', 'usb', 'hdmi', 'bluetooth', 'android', 'ios',
    'nvidia', 'intel', 'amd', 'ssd', 'ram', 'gpu', 'cpu', 'led', 'oled',
    'xiaomi', 'huawei', 'infinix', 'tecno', 'adidas', 'nike', 'lv', 'gucci',
}

DICT_URL = (
    "https://raw.githubusercontent.com/wolfgarbe/SymSpell/master/"
    "SymSpell.FrequencyDictionary/fr-100k.txt"
)
DICT_PATH = "/tmp/symspell_dict_fr.txt"


def load_symspell():
    global _sym_spell
    _sym_spell = SymSpell(max_dictionary_edit_distance=2, prefix_length=7)

    if not os.path.exists(DICT_PATH):
        logger.info("Téléchargement du dictionnaire SymSpell (timeout 15s)...")
        try:
            req = urllib.request.Request(DICT_URL)
            with urllib.request.urlopen(req, timeout=15) as resp:
                with open(DICT_PATH, "wb") as out:
                    out.write(resp.read())
            logger.info("Dictionnaire téléchargé.")
        except Exception as exc:
            logger.warning("Impossible de télécharger le dictionnaire SymSpell : %s — correction désactivée.", exc)
            _sym_spell = None
            return

    loaded = _sym_spell.load_dictionary(DICT_PATH, term_index=0, count_index=1)
    if loaded:
        logger.info("SymSpell chargé.")
    else:
        logger.warning("Échec du chargement du dictionnaire SymSpell.")


def correct_query(query: str) -> str:
    """Corrige les fautes de frappe mot par mot, en préservant les mots tech."""
    if _sym_spell is None:
        return query
    words = query.lower().split()
    corrected = []
    for word in words:
        if word in TECH_WORDS or len(word) <= 3:
            corrected.append(word)
        else:
            suggestions = _sym_spell.lookup(word, Verbosity.CLOSEST, max_edit_distance=2)
            corrected.append(suggestions[0].term if suggestions else word)
    return ' '.join(corrected)
