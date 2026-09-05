import importlib.util
import json
import tkinter as tk
from pathlib import Path
from tkinter import messagebox

DATA_FILE = Path(__file__).with_name("JCZXT_M.json")
MAX_DETECTABLE_M = 1048576


class TitleScreen:
    def __init__(self, root):
        self.root = root
        self.shop_window = None
        self.world_window = None
        self.bag_window = None
        self.m_coins = self.load_m_coins()
        root.title("JCZXT")
        root.geometry("700x620")
        root.minsize(520, 480)
        root.configure(bg="#f4f1ea")

        tk.Label(
            root,
            text="JCZXT_26.1B",
            bg="#f4f1ea",
            fg="#4b4b4b",
            font=("Microsoft YaHei UI", 10),
        ).place(x=14, y=12)
        tk.Label(
            root,
            text="XGstudio.Co_@2026_By_XG\\HBK\\LJH\\_XAM",
            bg="#f4f1ea",
            fg="#4b4b4b",
            font=("Microsoft YaHei UI", 9),
        ).place(relx=1.0, rely=1.0, x=-14, y=-12, anchor="se")
        tk.Label(
            root,
            text=self.m_coins_text(),
            bg="#f4f1ea",
            fg="#b23a48" if self.m_coins > MAX_DETECTABLE_M else "#4b4b4b",
            font=("Microsoft YaHei UI", 10),
        ).place(relx=1.0, x=-14, y=12, anchor="ne")

        title_area = tk.Frame(root, bg="#f4f1ea")
        title_area.pack(fill="x", pady=(82, 34))
        tk.Label(
            title_area,
            text="JCZXT",
            bg="#f4f1ea",
            fg="#171717",
            font=("Microsoft YaHei UI", 48, "bold"),
        ).pack()
        tk.Label(
            title_area,
            text="PYTHON_EDITION",
            bg="#f4f1ea",
            fg="#4b4b4b",
            font=("Microsoft YaHei UI", 24),
        ).pack(pady=(8, 0))

        options = tk.Frame(root, bg="#f4f1ea")
        options.pack(anchor="center")
        self.create_option(options, "世界", self.open_world)
        self.create_option(
            options,
            "商店",
            self.open_shop,
            disabled=self.m_coins > MAX_DETECTABLE_M,
        )
        self.create_option(options, "背包", self.open_bag)

    def load_m_coins(self):
        if not DATA_FILE.exists():
            DATA_FILE.write_text(
                json.dumps({"M币": 20}, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
        try:
            data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
            return int(data.get("M币", 0))
        except (OSError, ValueError, TypeError, json.JSONDecodeError):
            return MAX_DETECTABLE_M + 1

    def m_coins_text(self):
        if self.m_coins > MAX_DETECTABLE_M:
            return "M币：无法检测"
        return f"M币：{self.m_coins}"

    def create_option(self, parent, text, command, disabled=False):
        option_frame = tk.Frame(
            parent,
            bg="#f4f1ea",
            highlightbackground="#111111",
            highlightcolor="#111111",
            highlightthickness=2,
            width=260,
            height=60,
        )
        option_frame.pack(pady=8)
        option_frame.pack_propagate(False)
        tk.Button(
            option_frame,
            text=text,
            command=command,
            bg="#f4f1ea",
            fg="#111111",
            activebackground="#dedad1",
            activeforeground="#111111",
            relief="flat",
            bd=0,
            state="disabled" if disabled else "normal",
            font=("Microsoft YaHei UI", 16),
        ).pack(fill="both", expand=True)

    def not_available(self):
        messagebox.showinfo("提示", "这个功能还没写好。")

    def open_world(self):
        if self.world_window is not None and self.world_window.winfo_exists():
            self.world_window.lift()
            return

        world_path = Path(__file__).with_name("JCZXT_world.py")
        world_spec = importlib.util.spec_from_file_location("JCZXT_world", world_path)
        world_module = importlib.util.module_from_spec(world_spec)
        world_spec.loader.exec_module(world_module)

        self.world_window = tk.Toplevel(self.root)
        world_module.WorldApp(self.world_window)
        self.world_window.protocol("WM_DELETE_WINDOW", self.close_world)

    def close_world(self):
        if self.world_window is not None:
            self.world_window.destroy()
            self.world_window = None

    def open_bag(self):
        if self.bag_window is not None and self.bag_window.winfo_exists():
            self.bag_window.lift()
            return

        bag_path = Path(__file__).with_name("JCZXT_bag.py")
        bag_spec = importlib.util.spec_from_file_location("JCZXT_bag", bag_path)
        bag_module = importlib.util.module_from_spec(bag_spec)
        bag_spec.loader.exec_module(bag_module)
        self.bag_window = tk.Toplevel(self.root)
        bag_module.BagApp(self.bag_window)
        self.bag_window.protocol("WM_DELETE_WINDOW", self.close_bag)

    def close_bag(self):
        if self.bag_window is not None:
            self.bag_window.destroy()
            self.bag_window = None

    def open_shop(self):
        if self.shop_window is not None and self.shop_window.winfo_exists():
            self.shop_window.lift()
            return

        shop_path = Path(__file__).with_name("JCZXTshop.py")
        shop_spec = importlib.util.spec_from_file_location("JCZXTshop", shop_path)
        shop_module = importlib.util.module_from_spec(shop_spec)
        shop_spec.loader.exec_module(shop_module)

        self.shop_window = tk.Toplevel(self.root)
        shop_module.ShopApp(self.shop_window)
        self.shop_window.protocol("WM_DELETE_WINDOW", self.close_shop)

    def close_shop(self):
        if self.shop_window is not None:
            self.shop_window.destroy()
            self.shop_window = None
        self.root.deiconify()


if __name__ == "__main__":
    window = tk.Tk()
    TitleScreen(window)
    window.mainloop()
