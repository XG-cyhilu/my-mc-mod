import json
import tkinter as tk
from pathlib import Path


BAG_FILE = Path(__file__).with_name("JCZXT_bag_thing.json")


def load_bag():
    if not BAG_FILE.exists():
        BAG_FILE.write_text("{}", encoding="utf-8")
        return {}
    try:
        bag = json.loads(BAG_FILE.read_text(encoding="utf-8"))
        return bag if isinstance(bag, dict) else {}
    except (json.JSONDecodeError, OSError):
        return {}


class BagApp:
    def __init__(self, root):
        self.root = root
        root.title("JCZXT 背包")
        root.geometry("600x500")
        root.minsize(460, 360)
        root.configure(bg="#f3f5f7")

        header = tk.Frame(root, bg="#202b36")
        header.pack(fill="x")
        tk.Label(
            header,
            text="我的背包",
            bg="#202b36",
            fg="white",
            font=("Microsoft YaHei UI", 20, "bold"),
        ).pack(side="left", padx=24, pady=14)
        tk.Button(
            header,
            text="刷新",
            command=self.refresh,
            bg="#2a9d8f",
            fg="white",
            activebackground="#21867b",
            relief="flat",
            bd=0,
            padx=14,
            pady=6,
        ).pack(side="right", padx=20)

        self.content = tk.Frame(root, bg="#f3f5f7")
        self.content.pack(fill="both", expand=True, padx=20, pady=20)
        self.refresh()

    def refresh(self):
        for widget in self.content.winfo_children():
            widget.destroy()

        bag = load_bag()
        if not bag:
            tk.Label(
                self.content,
                text="背包还是空的",
                bg="#f3f5f7",
                fg="#52616b",
                font=("Microsoft YaHei UI", 16),
            ).pack(expand=True)
            return

        for item_name, quantity in bag.items():
            item_row = tk.Frame(
                self.content,
                bg="white",
                highlightbackground="#dce2e7",
                highlightthickness=1,
            )
            item_row.pack(fill="x", pady=5)
            tk.Label(
                item_row,
                text=item_name,
                bg="white",
                fg="#202b36",
                anchor="w",
                font=("Microsoft YaHei UI", 13, "bold"),
            ).pack(side="left", fill="x", expand=True, padx=16, pady=13)
            tk.Label(
                item_row,
                text=f"数量：{quantity}",
                bg="white",
                fg="#2a9d8f",
                font=("Microsoft YaHei UI", 12, "bold"),
            ).pack(side="right", padx=16)


if __name__ == "__main__":
    window = tk.Tk()
    BagApp(window)
    window.mainloop()
