import { FormEvent, useState } from 'react';
import { assistantMessages } from '@/constants/mockData';
import { useUIStore } from '@/store/uiStore';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { cn } from '@/utils/cn';

export const AssistantPanel = () => {
  const open = useUIStore((state) => state.assistantOpen);
  const [messages, setMessages] = useState(assistantMessages);
  const [draft, setDraft] = useState('');

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();

    if (!draft.trim()) return;

    setMessages((prev) => [
      ...prev,
      { id: crypto.randomUUID(), sender: 'user', content: draft, createdAt: 'Now' },
      {
        id: crypto.randomUUID(),
        sender: 'assistant',
        content: `Here is a quick HR insight for "${draft}". Backend integration can replace this simulated response with live analytics.`,
        createdAt: 'Now',
      },
    ]);
    setDraft('');
  };

  return (
    <aside
      className={cn(
        'fixed bottom-4 right-4 z-40 hidden w-full max-w-sm transition xl:block',
        open ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-4 opacity-0',
      )}
    >
      <Card className="p-0">
        <div className="rounded-t-3xl bg-brand-900 p-5 text-white">
          <p className="text-sm text-brand-100">AI HR Assistant</p>
          <h3 className="mt-1 text-lg font-semibold">Ask people and payroll questions</h3>
        </div>
        <div className="max-h-[420px] space-y-3 overflow-y-auto p-5">
          {messages.map((message) => (
            <div
              key={message.id}
              className={cn(
                'max-w-[85%] rounded-2xl px-4 py-3 text-sm',
                message.sender === 'assistant'
                  ? 'bg-slate-100 text-slate-800'
                  : 'ml-auto bg-brand-700 text-white',
              )}
            >
              <p>{message.content}</p>
              <p className="mt-1 text-[11px] opacity-70">{message.createdAt}</p>
            </div>
          ))}
        </div>
        <form onSubmit={handleSubmit} className="border-t border-slate-100 p-4">
          <div className="flex gap-2">
            <Input
              className="flex-1"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder='Try "Who came late today?"'
            />
            <Button type="submit">Send</Button>
          </div>
        </form>
      </Card>
    </aside>
  );
};
