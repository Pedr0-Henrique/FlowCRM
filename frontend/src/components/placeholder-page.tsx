export default function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-8">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-2 text-sm text-foreground/60">Módulo em construção — ver roadmap no README.</p>
    </div>
  );
}
